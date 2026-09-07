package com.example.evshare.service.impl;

import com.example.evshare.dto.request.AddGroupMemberRequest;
import com.example.evshare.dto.request.CreateOwnershipGroupRequest;
import com.example.evshare.dto.request.UpdateOwnershipGroupRequest;
import com.example.evshare.dto.response.OwnershipGroupResponse;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.OwnershipShare;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.OwnershipGroupRepository;
import com.example.evshare.repository.OwnershipShareRepository;
import com.example.evshare.repository.UserRepository;
import com.example.evshare.repository.VehicleRepository;
import com.example.evshare.service.OwnershipGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OwnershipGroupServiceImpl implements OwnershipGroupService {

    private static final Logger log = LoggerFactory.getLogger(OwnershipGroupServiceImpl.class);

    private final OwnershipGroupRepository ownershipGroupRepository;
    private final OwnershipShareRepository ownershipShareRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public OwnershipGroupServiceImpl(OwnershipGroupRepository ownershipGroupRepository,
                                     OwnershipShareRepository ownershipShareRepository,
                                     VehicleRepository vehicleRepository,
                                     UserRepository userRepository) {
        this.ownershipGroupRepository = ownershipGroupRepository;
        this.ownershipShareRepository = ownershipShareRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OwnershipGroupResponse createGroup(CreateOwnershipGroupRequest request) {
        log.info("Creating ownership group '{}' for vehicleId={}", request.getGroupName(), request.getVehicleId());

        // 1. Validate vehicle existence
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Vehicle not found with ID: %d", request.getVehicleId())));

        // 2. Validate vehicle 1:1 binding constraint (BR-OWN-01: exactly one group per vehicle)
        if (ownershipGroupRepository.existsByVehicleId(request.getVehicleId())) {
            throw new BusinessException(
                    String.format("Vehicle with ID %d is already bound to an active ownership group", request.getVehicleId()),
                    HttpStatus.CONFLICT
            );
        }

        // 3. Persist OwnershipGroup
        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName(request.getGroupName().trim());
        group.setVehicle(vehicle);
        group.setFormationDate(request.getFormationDate() != null ? request.getFormationDate() : LocalDate.now());
        group.setIsActive(true);

        OwnershipGroup savedGroup = ownershipGroupRepository.save(group);

        // 4. Enroll initial members if provided
        List<OwnershipShare> createdShares = new ArrayList<>();
        if (request.getMemberUserIds() != null && !request.getMemberUserIds().isEmpty()) {
            Set<Long> uniqueMemberIds = new LinkedHashSet<>(request.getMemberUserIds());
            for (Long userId : uniqueMemberIds) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException(String.format("User not found with ID: %d", userId)));

                OwnershipShare share = new OwnershipShare();
                share.setGroup(savedGroup);
                share.setUser(user);
                // Nominal placeholder percentage satisfying check constraint (> 0.00 and <= 100.00)
                // Note: Mathematical rebalancing to 100.00% is deferred to the 100% invariant checkpoint
                share.setPercentage(new BigDecimal("10.00"));
                share.setShareCertificateNumber(generateCertificateNumber(savedGroup.getId(), user.getId()));
                share.setIsActive(true);

                createdShares.add(ownershipShareRepository.save(share));
            }
        }

        log.info("Ownership group '{}' formed successfully with id={} and {} initial members",
                savedGroup.getGroupName(), savedGroup.getId(), createdShares.size());

        return OwnershipGroupResponse.fromEntity(savedGroup, createdShares);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnershipGroupResponse getGroupById(Long id) {
        OwnershipGroup group = ownershipGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", id)));

        List<OwnershipShare> shares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(id);
        return OwnershipGroupResponse.fromEntity(group, shares);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnershipGroupResponse> getMyGroups(Long userId) {
        log.debug("Fetching ownership groups for userId={}", userId);
        List<OwnershipGroup> groups = ownershipGroupRepository.findGroupsByUserId(userId);
        return groups.stream()
                .map(g -> {
                    List<OwnershipShare> shares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(g.getId());
                    return OwnershipGroupResponse.fromEntity(g, shares);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OwnershipGroupResponse updateGroup(Long id, UpdateOwnershipGroupRequest request) {
        log.info("Updating ownership group id={}", id);
        OwnershipGroup group = ownershipGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", id)));

        if (request.getGroupName() != null && !request.getGroupName().trim().isEmpty()) {
            group.setGroupName(request.getGroupName().trim());
        }
        if (request.getIsActive() != null) {
            group.setIsActive(request.getIsActive());
        }

        OwnershipGroup updated = ownershipGroupRepository.save(group);
        List<OwnershipShare> shares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(id);
        return OwnershipGroupResponse.fromEntity(updated, shares);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OwnershipGroupResponse addMember(Long groupId, AddGroupMemberRequest request) {
        log.info("Enrolling userId={} into groupId={}", request.getUserId(), groupId);
        OwnershipGroup group = ownershipGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Ownership group not found with ID: %d", groupId)));

        if (Boolean.FALSE.equals(group.getIsActive())) {
            throw new BusinessException("Cannot add members to an inactive ownership group", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(String.format("User not found with ID: %d", request.getUserId())));

        // Check if user is already enrolled (unique constraint uk_share_group_user)
        Optional<OwnershipShare> existingShareOpt = ownershipShareRepository.findByGroupIdAndUserId(groupId, request.getUserId());
        if (existingShareOpt.isPresent()) {
            OwnershipShare existingShare = existingShareOpt.get();
            if (Boolean.TRUE.equals(existingShare.getIsActive())) {
                throw new BusinessException(
                        String.format("User %d is already an active member of ownership group %d", request.getUserId(), groupId),
                        HttpStatus.CONFLICT
                );
            }
            // Reactivate membership
            existingShare.setIsActive(true);
            if (request.getPercentage() != null && request.getPercentage().compareTo(BigDecimal.ZERO) > 0) {
                existingShare.setPercentage(request.getPercentage());
            }
            ownershipShareRepository.save(existingShare);
        } else {
            OwnershipShare share = new OwnershipShare();
            share.setGroup(group);
            share.setUser(user);
            BigDecimal percentage = (request.getPercentage() != null && request.getPercentage().compareTo(BigDecimal.ZERO) > 0)
                    ? request.getPercentage()
                    : new BigDecimal("10.00");
            share.setPercentage(percentage);
            share.setShareCertificateNumber(generateCertificateNumber(groupId, user.getId()));
            share.setIsActive(true);
            ownershipShareRepository.save(share);
        }

        List<OwnershipShare> shares = ownershipShareRepository.findByGroupIdAndIsActiveTrue(groupId);
        return OwnershipGroupResponse.fromEntity(group, shares);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long groupId, Long userId) {
        log.info("Deactivating membership for userId={} in groupId={}", userId, groupId);
        OwnershipShare share = ownershipShareRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Membership record not found for user %d in group %d", userId, groupId)
                ));

        share.setIsActive(false);
        ownershipShareRepository.save(share);
    }

    private String generateCertificateNumber(Long groupId, Long userId) {
        return String.format("CERT-G%d-U%d-%s", groupId, userId, UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
