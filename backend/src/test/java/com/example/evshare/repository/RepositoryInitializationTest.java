package com.example.evshare.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RepositoryInitializationTest {

    // Tier 1: Identity & Users (5)
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private IdentityVerificationRepository identityVerificationRepository;
    @Autowired private DriverLicenseRepository driverLicenseRepository;

    // Tier 2: Vehicles & Ownership (3)
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;

    // Tier 3: Contracts & Signatures (2)
    @Autowired private CoOwnershipContractRepository coOwnershipContractRepository;
    @Autowired private ContractSignatureRepository contractSignatureRepository;

    // Tier 4: Bookings, Usage Sessions, Inspections & Services (4)
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UsageSessionRepository usageSessionRepository;
    @Autowired private VehicleInspectionRepository vehicleInspectionRepository;
    @Autowired private VehicleServiceRepository vehicleServiceRepository;

    // Tier 5: Financial Operations, Funds, Expenses & Payments (5)
    @Autowired private SharedFundRepository sharedFundRepository;
    @Autowired private FundTransactionRepository fundTransactionRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private ExpenseAllocationRepository expenseAllocationRepository;
    @Autowired private PaymentRepository paymentRepository;

    // Tier 6: Governance, Proposals, Votes & Disputes (5)
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private DisputeRepository disputeRepository;
    @Autowired private DisputeEvidenceRepository disputeEvidenceRepository;

    // Tier 7: Intelligence, Notifications & Audit (3)
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AiRecommendationRepository aiRecommendationRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("All 27 Spring Data JPA repository beans must be instantiated and injected")
    void testAll27RepositoriesInjected() {
        // Tier 1
        assertNotNull(userRepository);
        assertNotNull(roleRepository);
        assertNotNull(userRoleRepository);
        assertNotNull(identityVerificationRepository);
        assertNotNull(driverLicenseRepository);

        // Tier 2
        assertNotNull(vehicleRepository);
        assertNotNull(ownershipGroupRepository);
        assertNotNull(ownershipShareRepository);

        // Tier 3
        assertNotNull(coOwnershipContractRepository);
        assertNotNull(contractSignatureRepository);

        // Tier 4
        assertNotNull(bookingRepository);
        assertNotNull(usageSessionRepository);
        assertNotNull(vehicleInspectionRepository);
        assertNotNull(vehicleServiceRepository);

        // Tier 5
        assertNotNull(sharedFundRepository);
        assertNotNull(fundTransactionRepository);
        assertNotNull(expenseRepository);
        assertNotNull(expenseAllocationRepository);
        assertNotNull(paymentRepository);

        // Tier 6
        assertNotNull(proposalRepository);
        assertNotNull(voteOptionRepository);
        assertNotNull(voteRepository);
        assertNotNull(disputeRepository);
        assertNotNull(disputeEvidenceRepository);

        // Tier 7
        assertNotNull(notificationRepository);
        assertNotNull(aiRecommendationRepository);
        assertNotNull(auditLogRepository);
    }

    @Test
    @DisplayName("All 27 Spring Data JPA repositories should execute count queries against live database")
    void testAll27RepositoriesQueryExecution() {
        // Tier 1
        assertTrue(userRepository.count() >= 0);
        assertTrue(roleRepository.count() >= 0);
        assertTrue(userRoleRepository.count() >= 0);
        assertTrue(identityVerificationRepository.count() >= 0);
        assertTrue(driverLicenseRepository.count() >= 0);

        // Tier 2
        assertTrue(vehicleRepository.count() >= 0);
        assertTrue(ownershipGroupRepository.count() >= 0);
        assertTrue(ownershipShareRepository.count() >= 0);

        // Tier 3
        assertTrue(coOwnershipContractRepository.count() >= 0);
        assertTrue(contractSignatureRepository.count() >= 0);

        // Tier 4
        assertTrue(bookingRepository.count() >= 0);
        assertTrue(usageSessionRepository.count() >= 0);
        assertTrue(vehicleInspectionRepository.count() >= 0);
        assertTrue(vehicleServiceRepository.count() >= 0);

        // Tier 5
        assertTrue(sharedFundRepository.count() >= 0);
        assertTrue(fundTransactionRepository.count() >= 0);
        assertTrue(expenseRepository.count() >= 0);
        assertTrue(expenseAllocationRepository.count() >= 0);
        assertTrue(paymentRepository.count() >= 0);

        // Tier 6
        assertTrue(proposalRepository.count() >= 0);
        assertTrue(voteOptionRepository.count() >= 0);
        assertTrue(voteRepository.count() >= 0);
        assertTrue(disputeRepository.count() >= 0);
        assertTrue(disputeEvidenceRepository.count() >= 0);

        // Tier 7
        assertTrue(notificationRepository.count() >= 0);
        assertTrue(aiRecommendationRepository.count() >= 0);
        assertTrue(auditLogRepository.count() >= 0);
    }
}
