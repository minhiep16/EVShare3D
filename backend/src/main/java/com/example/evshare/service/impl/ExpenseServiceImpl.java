package com.example.evshare.service.impl;

import com.example.evshare.dto.request.CreateExpenseRequest;
import com.example.evshare.dto.response.ExpenseAllocationResponse;
import com.example.evshare.dto.response.ExpenseAuditLogResponse;
import com.example.evshare.dto.response.ExpenseResponse;
import com.example.evshare.dto.response.PagedData;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.*;
import com.example.evshare.security.OwnershipSecurity;
import com.example.evshare.service.ExpenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class ExpenseServiceImpl implements ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);
    private static final BigDecimal EVIDENCE_THRESHOLD = new BigDecimal("1000000.00");

    private final ExpenseRepository expenseRepository;
    private final ExpenseAllocationRepository expenseAllocationRepository;
    private final OwnershipGroupRepository ownershipGroupRepository;
    private final OwnershipShareRepository ownershipShareRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final SharedFundRepository sharedFundRepository;
    private final AuditLogRepository auditLogRepository;
    private final OwnershipSecurity ownershipSecurity;
    private final ObjectMapper objectMapper;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                              ExpenseAllocationRepository expenseAllocationRepository,
                              OwnershipGroupRepository ownershipGroupRepository,
                              OwnershipShareRepository ownershipShareRepository,
                              VehicleRepository vehicleRepository,
                              UserRepository userRepository,
                              SharedFundRepository sharedFundRepository,
                              AuditLogRepository auditLogRepository,
                              OwnershipSecurity ownershipSecurity,
                              ObjectMapper objectMapper) {
        this.expenseRepository = expenseRepository;
        this.expenseAllocationRepository = expenseAllocationRepository;
        this.ownershipGroupRepository = ownershipGroupRepository;
        this.ownershipShareRepository = ownershipShareRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.sharedFundRepository = sharedFundRepository;
        this.auditLogRepository = auditLogRepository;
        this.ownershipSecurity = ownershipSecurity;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExpenseResponse createExpense(CreateExpenseRequest request, Long currentUserId, String ipAddress) {
        log.info("Processing expense creation request: group={}, category={}, amount={}, user={}",
                request.getGroupId(), request.getCategory(), request.getAmount(), currentUserId);

        // 1. Validate creator
        if (currentUserId == null) {
            throw new BusinessException("Authenticated user identifier is required", HttpStatus.UNAUTHORIZED);
        }
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));
        if (Boolean.FALSE.equals(creator.getIsActive())) {
            throw new BusinessException("User account is inactive", HttpStatus.FORBIDDEN);
        }

        // 2. Validate Ownership Group
        if (request.getGroupId() == null) {
            throw new BusinessException("Ownership group ID is mandatory", HttpStatus.BAD_REQUEST);
        }
        OwnershipGroup group = ownershipGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Ownership group not found with ID: " + request.getGroupId()));
        if (Boolean.FALSE.equals(group.getIsActive())) {
            throw new BusinessException("Ownership group [" + group.getGroupName() + "] is inactive", HttpStatus.BAD_REQUEST);
        }

        // 3. Authorization Check on Creator
        boolean isStaffOrAdmin = creator.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_STAFF || r.getName() == RoleName.ROLE_ADMIN);
        if (!isStaffOrAdmin) {
            boolean isMember = ownershipSecurity.isGroupMember(group.getId(), creator.getId());
            if (!isMember) {
                throw new BusinessException("User is not an active co-owner of syndicate group [" + group.getId() + "]", HttpStatus.FORBIDDEN);
            }
        }

        // 4. Validate Vehicle
        Vehicle vehicle;
        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + request.getVehicleId()));
            if (group.getVehicle() == null || !group.getVehicle().getId().equals(vehicle.getId())) {
                throw new BusinessException("Specified vehicle [" + vehicle.getId() + "] does not belong to syndicate group [" + group.getId() + "]", HttpStatus.BAD_REQUEST);
            }
        } else {
            vehicle = group.getVehicle();
            if (vehicle == null) {
                throw new BusinessException("Ownership group does not have an assigned vehicle", HttpStatus.BAD_REQUEST);
            }
        }

        // 5. Validate Amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Expense amount must be strictly greater than 0.00", HttpStatus.BAD_REQUEST);
        }
        BigDecimal normalizedAmount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);

        // 6. Validate Currency
        String currency = request.getCurrency() != null && !request.getCurrency().trim().isEmpty()
                ? request.getCurrency().trim().toUpperCase()
                : "VND";
        if (!currency.matches("^[A-Z]{3,4}$")) {
            throw new BusinessException("Invalid currency format: " + currency, HttpStatus.BAD_REQUEST);
        }
        Optional<SharedFund> fundOpt = sharedFundRepository.findByGroupId(group.getId());
        if (fundOpt.isPresent() && !fundOpt.get().getCurrency().equalsIgnoreCase(currency)) {
            throw new BusinessException("Currency [" + currency + "] does not match syndicate fund currency [" + fundOpt.get().getCurrency() + "]", HttpStatus.BAD_REQUEST);
        }

        // 7. Validate Category
        if (request.getCategory() == null) {
            throw new BusinessException("Expense category is mandatory", HttpStatus.BAD_REQUEST);
        }

        // 8. Validate Incurred Date
        LocalDate incurredDate = request.getIncurredDate();
        if (incurredDate == null) {
            throw new BusinessException("Incurred date is mandatory", HttpStatus.BAD_REQUEST);
        }
        if (incurredDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Expense incurred date cannot be in the future", HttpStatus.BAD_REQUEST);
        }
        if (group.getFormationDate() != null && incurredDate.isBefore(group.getFormationDate())) {
            throw new BusinessException("Expense incurred date [" + incurredDate + "] cannot precede group formation date [" + group.getFormationDate() + "]", HttpStatus.BAD_REQUEST);
        }

        // 9. Validate Evidence Where Required
        boolean evidenceMandatory = request.getCategory().isEvidenceRequired()
                || normalizedAmount.compareTo(EVIDENCE_THRESHOLD) >= 0
                || Boolean.TRUE.equals(request.getEvidenceRequired());

        boolean hasInvoice = request.getInvoiceReference() != null && !request.getInvoiceReference().trim().isEmpty();
        boolean hasEvidenceUrl = request.getEvidenceUrl() != null && !request.getEvidenceUrl().trim().isEmpty();

        if (evidenceMandatory && !hasInvoice && !hasEvidenceUrl) {
            throw new BusinessException("Receipt or invoice evidence is strictly required for "
                    + request.getCategory() + " expenses or amounts exceeding " + EVIDENCE_THRESHOLD + " " + currency, HttpStatus.BAD_REQUEST);
        }

        // 10. Duplicate Detection
        if (hasInvoice) {
            String invRef = request.getInvoiceReference().trim();
            if (expenseRepository.existsByInvoiceReference(invRef)) {
                throw new BusinessException("Duplicate expense detected: Invoice reference [" + invRef + "] has already been recorded in platform ledger", HttpStatus.CONFLICT);
            }
        }
        boolean isDuplicate = expenseRepository.existsDuplicateExpense(
                group.getId(),
                vehicle.getId(),
                request.getCategory(),
                normalizedAmount,
                incurredDate,
                request.getTitle().trim()
        );
        if (isDuplicate) {
            throw new BusinessException("Duplicate expense detected: An identical expense for vehicle ["
                    + vehicle.getId() + "] with category [" + request.getCategory() + "], amount [" + normalizedAmount
                    + "], date [" + incurredDate + "], and title [" + request.getTitle().trim() + "] already exists", HttpStatus.CONFLICT);
        }

        // 11. Persist Expense Entity
        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setVehicle(vehicle);
        expense.setTitle(request.getTitle().trim());
        expense.setCategory(request.getCategory());
        expense.setTotalAmount(normalizedAmount);
        expense.setCurrency(currency);
        expense.setAllocationStrategy(request.getAllocationStrategy() != null ? request.getAllocationStrategy() : AllocationStrategy.OWNERSHIP_BASED);
        expense.setInvoiceReference(hasInvoice ? request.getInvoiceReference().trim() : null);
        expense.setEvidenceUrl(hasEvidenceUrl ? request.getEvidenceUrl().trim() : null);
        expense.setLoggedByUser(creator);
        expense.setIncurredDate(incurredDate);
        expense.setCreatedAt(Instant.now());

        Expense savedExpense = expenseRepository.save(expense);

        // NOTE: In compliance with Checkpoint 06-C ("Do not allocate costs yet"),
        // cost allocations are decoupled and will be handled in subsequent checkpoints.

        // 12. Record Audit Log for Historical Auditability
        recordAuditLog(savedExpense, creator, "EXPENSE_CREATED", ipAddress);

        log.info("Expense successfully recorded with ID [{}] for syndicate [{}]", savedExpense.getId(), group.getId());
        return ExpenseResponse.fromEntity(savedExpense);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id, Long currentUserId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        verifyGroupReadAccess(expense.getGroup().getId(), currentUserId);
        return ExpenseResponse.fromEntity(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedData<ExpenseResponse> getGroupExpenses(Long groupId, ExpenseCategory category,
                                                       LocalDate startDate, LocalDate endDate,
                                                       Pageable pageable, Long currentUserId) {
        verifyGroupReadAccess(groupId, currentUserId);

        Page<Expense> page;
        if (category != null && startDate != null && endDate != null) {
            page = expenseRepository.findByGroupIdAndCategoryAndIncurredDateBetween(groupId, category, startDate, endDate, pageable);
        } else if (category != null) {
            page = expenseRepository.findByGroupIdAndCategory(groupId, category, pageable);
        } else if (startDate != null && endDate != null) {
            page = expenseRepository.findByGroupIdAndIncurredDateBetween(groupId, startDate, endDate, pageable);
        } else {
            page = expenseRepository.findByGroupId(groupId, pageable);
        }

        List<ExpenseResponse> content = page.getContent().stream()
                .map(ExpenseResponse::fromEntity)
                .collect(Collectors.toList());

        return PagedData.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseAuditLogResponse> getExpenseHistory(Long id, Long currentUserId) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with ID: " + id));

        verifyGroupReadAccess(expense.getGroup().getId(), currentUserId);

        List<AuditLog> auditLogs = auditLogRepository.findByEntityNameAndEntityIdOrderByIdDesc("Expense", id);
        return auditLogs.stream()
                .map(ExpenseAuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void verifyGroupReadAccess(Long groupId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("Authentication required", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + currentUserId));

        boolean isStaffOrAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_STAFF || r.getName() == RoleName.ROLE_ADMIN);

        if (!isStaffOrAdmin) {
            boolean isMember = ownershipSecurity.isGroupMember(groupId, currentUserId);
            if (!isMember) {
                throw new BusinessException("Access denied: User is not an active co-owner of this syndicate", HttpStatus.FORBIDDEN);
            }
        }
    }

    private void recordAuditLog(Expense expense, User actor, String action, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUser(actor);
            auditLog.setAction(action);
            auditLog.setEntityName("Expense");
            auditLog.setEntityId(expense.getId());
            auditLog.setIpAddress(ipAddress);

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("expenseId", expense.getId());
            details.put("groupId", expense.getGroup().getId());
            details.put("groupName", expense.getGroup().getGroupName());
            details.put("vehicleId", expense.getVehicle() != null ? expense.getVehicle().getId() : null);
            details.put("title", expense.getTitle());
            details.put("category", expense.getCategory().name());
            details.put("totalAmount", expense.getTotalAmount());
            details.put("currency", expense.getCurrency());
            details.put("allocationStrategy", expense.getAllocationStrategy().name());
            details.put("incurredDate", expense.getIncurredDate().toString());
            details.put("invoiceReference", expense.getInvoiceReference());
            details.put("evidenceUrl", expense.getEvidenceUrl());
            details.put("actorId", actor.getId());
            details.put("actorName", actor.getFullName());

            auditLog.setNewStateJson(objectMapper.writeValueAsString(details));
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to serialize audit log for expense ID [{}]: {}", expense.getId(), ex.getMessage(), ex);
        }
    }
}
