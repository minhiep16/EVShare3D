package com.example.evshare.service.impl;

import com.example.evshare.dto.request.CreateVehicleRequest;
import com.example.evshare.dto.request.UpdateVehicleStatusRequest;
import com.example.evshare.dto.response.PagedData;
import com.example.evshare.dto.response.VehicleResponse;
import com.example.evshare.dto.response.VehicleTelemetryResponse;
import com.example.evshare.entity.AuditLog;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.AuditLogRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VehicleRepository;
import com.example.evshare.security.SecurityRoles;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.VehicleService;
import com.example.evshare.service.VehicleStateMachine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VehicleServiceImpl implements VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleServiceImpl.class);

    private final VehicleRepository vehicleRepository;
    private final VehicleStateMachine vehicleStateMachine;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              VehicleStateMachine vehicleStateMachine,
                              AuditLogRepository auditLogRepository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleStateMachine = vehicleStateMachine;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        log.info("Registering new Digital Twin EV: VIN={}, plate={}, model={}",
                request.getVin(), request.getLicensePlate(), request.getModelName());

        String trimmedVin = request.getVin().trim();
        String trimmedPlate = request.getLicensePlate().trim();

        if (vehicleRepository.existsByVin(trimmedVin)) {
            throw new BusinessException(String.format("Vehicle with VIN '%s' already exists", trimmedVin), HttpStatus.CONFLICT);
        }
        if (vehicleRepository.existsByLicensePlate(trimmedPlate)) {
            throw new BusinessException(String.format("Vehicle with license plate '%s' already exists", trimmedPlate), HttpStatus.CONFLICT);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setVin(trimmedVin);
        vehicle.setLicensePlate(trimmedPlate);
        vehicle.setModelName(request.getModelName().trim());
        vehicle.setManufacturer(request.getManufacturer().trim());
        vehicle.setModel3dAssetPath(request.getModel3dAssetPath().trim());
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(request.getBatteryLevel() != null ? request.getBatteryLevel() : 100);
        vehicle.setOdometerKm(request.getOdometerKm() != null ? request.getOdometerKm() : BigDecimal.ZERO);
        vehicle.setStallLocationCode(request.getStallLocationCode() != null && !request.getStallLocationCode().isBlank()
                ? request.getStallLocationCode().trim()
                : "BAY-01");

        Vehicle saved = vehicleRepository.save(vehicle);

        // Record audit trail
        recordAudit("CREATE_VEHICLE", saved.getId(), null, serializeVehicleSnapshot(saved));
        log.info("Successfully registered vehicle ID={} (VIN={})", saved.getId(), saved.getVin());

        return VehicleResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedData<VehicleResponse> getVehicles(VehicleStatus status, String manufacturer, Pageable pageable) {
        Page<Vehicle> page;
        boolean hasStatus = status != null;
        boolean hasManufacturer = manufacturer != null && !manufacturer.isBlank();

        if (hasStatus && hasManufacturer) {
            page = vehicleRepository.findByStatusAndManufacturerIgnoreCase(status, manufacturer.trim(), pageable);
        } else if (hasStatus) {
            page = vehicleRepository.findByStatus(status, pageable);
        } else if (hasManufacturer) {
            page = vehicleRepository.findByManufacturerIgnoreCase(manufacturer.trim(), pageable);
        } else {
            page = vehicleRepository.findAll(pageable);
        }

        List<VehicleResponse> items = page.getContent().stream()
                .map(VehicleResponse::fromEntity)
                .collect(Collectors.toList());

        return new PagedData<>(items, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTelemetryResponse getVehicleTelemetry(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Vehicle not found with ID: %d", vehicleId)));
        return VehicleTelemetryResponse.fromEntity(vehicle);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleResponse updateVehicleStatus(Long vehicleId, UpdateVehicleStatusRequest request) {
        log.info("Attempting state transition for vehicleId={} to targetStatus={}", vehicleId, request.getStatus());

        // 1. Transactional pessimistic row lock to eliminate concurrency race conditions
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Vehicle not found with ID: %d", vehicleId)));

        VehicleStatus currentStatus = vehicle.getStatus();
        VehicleStatus targetStatus = request.getStatus();

        // 2. Validate state transition according to state machine rules
        vehicleStateMachine.validateTransition(currentStatus, targetStatus);

        // 3. Fine-grained RBAC verification: UNAVAILABLE status is strictly restricted to ADMIN
        if (targetStatus == VehicleStatus.UNAVAILABLE) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(SecurityRoles.ROLE_ADMIN) || a.getAuthority().equals(SecurityRoles.ADMIN));
            if (!isAdmin) {
                log.warn("Unauthorized attempt to transition vehicleId={} to UNAVAILABLE by non-admin user", vehicleId);
                throw new AccessDeniedException("Only platform administrators can transition a vehicle to UNAVAILABLE status");
            }
        }

        String oldJson = serializeVehicleSnapshot(vehicle);

        // 4. Update status and persist transactionally
        vehicle.setStatus(targetStatus);
        Vehicle saved = vehicleRepository.save(vehicle);

        recordAudit("TRANSITION_VEHICLE_STATUS", saved.getId(), oldJson, serializeVehicleSnapshot(saved));

        log.info("Successfully transitioned vehicleId={} from {} to {} (reason='{}')",
                vehicleId, currentStatus, targetStatus, request.getReason() != null ? request.getReason() : "N/A");

        return VehicleResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Vehicle not found with ID: %d", id)));
        return VehicleResponse.fromEntity(vehicle);
    }

    private String serializeVehicleSnapshot(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", vehicle.getId());
        map.put("vin", vehicle.getVin());
        map.put("licensePlate", vehicle.getLicensePlate());
        map.put("modelName", vehicle.getModelName());
        map.put("manufacturer", vehicle.getManufacturer());
        map.put("status", vehicle.getStatus() != null ? vehicle.getStatus().name() : null);
        map.put("batteryLevel", vehicle.getBatteryLevel());
        map.put("odometerKm", vehicle.getOdometerKm());
        map.put("stallLocationCode", vehicle.getStallLocationCode());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Failed to serialize vehicle snapshot for ID={}", vehicle.getId(), e);
            return "{}";
        }
    }

    private void recordAudit(String action, Long vehicleId, String oldStateJson, String newStateJson) {
        User currentUser = getCurrentUserOrNull();
        AuditLog logEntry = new AuditLog();
        logEntry.setUser(currentUser);
        logEntry.setAction(action);
        logEntry.setEntityName("Vehicle");
        logEntry.setEntityId(vehicleId);
        logEntry.setOldStateJson(oldStateJson);
        logEntry.setNewStateJson(newStateJson);
        logEntry.setCreatedAt(Instant.now());
        auditLogRepository.save(logEntry);
    }

    private User getCurrentUserOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                return userRepository.findById(principal.getId()).orElse(null);
            }
        } catch (Exception e) {
            log.debug("No authenticated user in context for audit log: {}", e.getMessage());
        }
        return null;
    }
}
