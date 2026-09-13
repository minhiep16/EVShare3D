package com.example.evshare.service;

import com.example.evshare.dto.request.AddMediationNotesRequest;
import com.example.evshare.dto.request.AdminArbitrateDisputeRequest;
import com.example.evshare.dto.request.CreateDisputeEvidenceRequest;
import com.example.evshare.dto.request.CreateDisputeRequest;
import com.example.evshare.dto.request.DisputeFundAdjustmentRequest;
import com.example.evshare.dto.request.ProposeResolutionRequest;
import com.example.evshare.dto.response.DisputeArbitrationDossierResponse;
import com.example.evshare.dto.response.DisputeEvidenceResponse;
import com.example.evshare.dto.response.DisputeResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.DisputeStatus;
import com.example.evshare.entity.enums.FundTransactionSource;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.TransactionEntryType;
import com.example.evshare.entity.enums.TransactionType;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InsufficientFundBalanceException;
import com.example.evshare.exception.InvalidDisputeStateTransitionException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.*;
import com.example.evshare.service.impl.DisputeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private DisputeEvidenceRepository disputeEvidenceRepository;

    @Mock
    private OwnershipGroupRepository ownershipGroupRepository;

    @Mock
    private OwnershipShareRepository ownershipShareRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsageSessionRepository usageSessionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SharedFundRepository sharedFundRepository;

    @Mock
    private FundTransactionRepository fundTransactionRepository;

    private DisputeStateMachine disputeStateMachine;
    private DisputeService disputeService;

    private User complainant;
    private User respondent;
    private User staffUser;
    private User adminUser;
    private OwnershipGroup sampleGroup;
    private Vehicle sampleVehicle;
    private SharedFund sampleSharedFund;

    @BeforeEach
    void setUp() {
        disputeStateMachine = new DisputeStateMachine();
        disputeService = new DisputeServiceImpl(
                disputeRepository,
                disputeEvidenceRepository,
                ownershipGroupRepository,
                ownershipShareRepository,
                userRepository,
                usageSessionRepository,
                auditLogRepository,
                disputeStateMachine,
                sharedFundRepository,
                fundTransactionRepository
        );

        Role roleCoOwner = new Role(1L, RoleName.ROLE_CO_OWNER);
        Role roleStaff = new Role(2L, RoleName.ROLE_STAFF);
        Role roleAdmin = new Role(3L, RoleName.ROLE_ADMIN);

        complainant = new User();
        complainant.setId(10L);
        complainant.setEmail("alice@evshare.io");
        complainant.setFullName("Alice Complainant");
        complainant.setIsActive(true);
        complainant.getRoles().add(roleCoOwner);

        respondent = new User();
        respondent.setId(20L);
        respondent.setEmail("bob@evshare.io");
        respondent.setFullName("Bob Respondent");
        respondent.setIsActive(true);
        respondent.getRoles().add(roleCoOwner);

        staffUser = new User();
        staffUser.setId(30L);
        staffUser.setEmail("staff@evshare.io");
        staffUser.setFullName("Staff Operator");
        staffUser.setIsActive(true);
        staffUser.getRoles().add(roleStaff);

        adminUser = new User();
        adminUser.setId(60L);
        adminUser.setEmail("admin@evshare.io");
        adminUser.setFullName("Admin Arbitrator");
        adminUser.setIsActive(true);
        adminUser.getRoles().add(roleAdmin);

        sampleVehicle = new Vehicle();
        sampleVehicle.setId(100L);
        sampleVehicle.setModelName("VinFast VF8");

        sampleGroup = new OwnershipGroup();
        sampleGroup.setId(1L);
        sampleGroup.setGroupName("Syndicate Alpha");
        sampleGroup.setVehicle(sampleVehicle);
        sampleGroup.setIsActive(true);

        sampleSharedFund = new SharedFund();
        sampleSharedFund.setId(10L);
        sampleSharedFund.setGroup(sampleGroup);
        sampleSharedFund.setCurrentBalance(new BigDecimal("5000000.00"));
        sampleSharedFund.setCurrency("VND");
        sampleSharedFund.setUpdatedAt(Instant.now());
    }

    private CreateDisputeRequest createValidRequest() {
        CreateDisputeEvidenceRequest ev = new CreateDisputeEvidenceRequest(
                "https://storage.evshare.io/evidences/scratch1.jpg",
                "{\"x\": 1.0, \"y\": 2.0}",
                "Bumper damage photo"
        );
        return new CreateDisputeRequest(
                1L,
                null,
                20L,
                "Vehicle returned with front bumper scratch",
                "During check-out inspection, deep scratches were observed on the front right bumper.",
                new ArrayList<>(List.of(ev))
        );
    }

    private OwnershipShare createShare(Long id, OwnershipGroup group, User user, BigDecimal percentage, boolean isActive) {
        return new OwnershipShare(id, group, user, percentage, "CERT-" + id, Instant.now(), isActive);
    }

    @Nested
    @DisplayName("07-K: Creator Validation Tests")
    class CreatorValidationTests {

        @Test
        @DisplayName("Valid active co-owner can file dispute")
        void testCreateDispute_activeCoOwner_success() {
            CreateDisputeRequest req = createValidRequest();
            OwnershipShare share = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), true);

            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share));
            when(userRepository.findById(20L)).thenReturn(Optional.of(respondent));
            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(500L);
                return d;
            });
            when(disputeEvidenceRepository.saveAllAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse response = disputeService.createDispute(req, 10L);

            assertNotNull(response);
            assertEquals(500L, response.getId());
            assertEquals(DisputeStatus.OPEN, response.getStatus());
            assertEquals("Vehicle returned with front bumper scratch", response.getTitle());
            assertEquals(1, response.getEvidences().size());
            verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Non-member outsider is rejected with HTTP 403 Forbidden")
        void testCreateDispute_outsider_rejected() {
            CreateDisputeRequest req = createValidRequest();

            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("not a co-owner"));
        }

        @Test
        @DisplayName("Inactive co-owner (isActive == false) is rejected with HTTP 403 Forbidden")
        void testCreateDispute_inactiveCoOwner_rejected() {
            CreateDisputeRequest req = createValidRequest();
            OwnershipShare inactiveShare = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), false);

            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(inactiveShare));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("inactive co-owner"));
        }

        @Test
        @DisplayName("Platform staff can file dispute without holding syndicate share")
        void testCreateDispute_staff_success() {
            CreateDisputeRequest req = createValidRequest();

            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(20L)).thenReturn(Optional.of(respondent));
            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(501L);
                return d;
            });
            when(disputeEvidenceRepository.saveAllAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse response = disputeService.createDispute(req, 30L);

            assertNotNull(response);
            assertEquals(501L, response.getId());
            verify(ownershipShareRepository, never()).findByGroupIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Inactive user account is rejected with HTTP 403 Forbidden")
        void testCreateDispute_inactiveAccount_rejected() {
            CreateDisputeRequest req = createValidRequest();
            complainant.setIsActive(false);
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("account is inactive"));
        }
    }

    @Nested
    @DisplayName("07-K: Ownership Group Validation Tests")
    class OwnershipGroupValidationTests {

        @Test
        @DisplayName("Non-existent ownership group throws ResourceNotFoundException")
        void testCreateDispute_nonExistentGroup() {
            CreateDisputeRequest req = createValidRequest();
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> disputeService.createDispute(req, 10L));
        }

        @Test
        @DisplayName("Inactive ownership group throws BusinessException")
        void testCreateDispute_inactiveGroup() {
            CreateDisputeRequest req = createValidRequest();
            sampleGroup.setIsActive(false);

            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertTrue(ex.getMessage().contains("is not active"));
        }
    }

    @Nested
    @DisplayName("07-K: Related Entity Validation Tests")
    class RelatedEntityValidationTests {

        @Test
        @DisplayName("Complainant cannot file dispute against themselves (self-dispute rejected)")
        void testCreateDispute_selfDispute_rejected() {
            CreateDisputeRequest req = createValidRequest();
            req.setRespondentUserId(10L); // Same as complainant

            OwnershipShare share = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("cannot file a dispute against themselves"));
        }

        @Test
        @DisplayName("Usage session belonging to a different vehicle throws BusinessException")
        void testCreateDispute_mismatchedUsageSession() {
            CreateDisputeRequest req = createValidRequest();
            req.setUsageSessionId(999L);

            Vehicle differentVehicle = new Vehicle();
            differentVehicle.setId(200L); // Differs from sampleVehicle (100L)

            Booking diffBooking = new Booking();
            diffBooking.setId(888L);
            diffBooking.setVehicle(differentVehicle);

            UsageSession diffSession = new UsageSession();
            diffSession.setId(999L);
            diffSession.setBooking(diffBooking);

            OwnershipShare share = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share));
            when(usageSessionRepository.findById(999L)).thenReturn(Optional.of(diffSession));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertTrue(ex.getMessage().contains("does not belong to group's vehicle"));
        }

        @Test
        @DisplayName("Valid usage session with matching vehicle succeeds")
        void testCreateDispute_validMatchingUsageSession() {
            CreateDisputeRequest req = createValidRequest();
            req.setUsageSessionId(105L);

            Booking matchingBooking = new Booking();
            matchingBooking.setId(777L);
            matchingBooking.setVehicle(sampleVehicle);

            UsageSession matchingSession = new UsageSession();
            matchingSession.setId(105L);
            matchingSession.setBooking(matchingBooking);

            OwnershipShare share = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share));
            when(usageSessionRepository.findById(105L)).thenReturn(Optional.of(matchingSession));
            when(userRepository.findById(20L)).thenReturn(Optional.of(respondent));
            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(505L);
                return d;
            });
            when(disputeEvidenceRepository.saveAllAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse response = disputeService.createDispute(req, 10L);

            assertNotNull(response);
            assertEquals(105L, response.getUsageSessionId());
        }
    }

    @Nested
    @DisplayName("07-K: Reason & Description Validation Tests")
    class ReasonValidationTests {

        @Test
        @DisplayName("Blank title throws BusinessException")
        void testCreateDispute_blankTitle() {
            CreateDisputeRequest req = createValidRequest();
            req.setTitle("   ");

            OwnershipShare share = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertTrue(ex.getMessage().contains("title cannot be blank"));
        }

        @Test
        @DisplayName("Blank description throws BusinessException")
        void testCreateDispute_blankDescription() {
            CreateDisputeRequest req = createValidRequest();
            req.setDescription("");

            OwnershipShare share = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertTrue(ex.getMessage().contains("description cannot be blank"));
        }
    }

    @Nested
    @DisplayName("07-K: Evidence Requirement Validation Tests")
    class EvidenceRequirementTests {

        @Test
        @DisplayName("Filing dispute with empty evidence list is rejected (HTTP 400 Bad Request)")
        void testCreateDispute_emptyEvidenceList_rejected() {
            CreateDisputeRequest req = createValidRequest();
            req.setEvidences(new ArrayList<>()); // Empty

            OwnershipShare share = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share));
            when(userRepository.findById(20L)).thenReturn(Optional.of(respondent));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("At least one evidence item"));
        }

        @Test
        @DisplayName("Evidence item with blank fileUrl is rejected (HTTP 400 Bad Request)")
        void testCreateDispute_blankFileUrl_rejected() {
            CreateDisputeRequest req = createValidRequest();
            req.getEvidences().get(0).setFileUrl("  "); // Blank URL

            OwnershipShare share = createShare(1L, sampleGroup, complainant, new BigDecimal("40.00"), true);
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 10L)).thenReturn(Optional.of(share));
            when(userRepository.findById(20L)).thenReturn(Optional.of(respondent));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.createDispute(req, 10L));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("file URL cannot be blank"));
        }
    }

    @Nested
    @DisplayName("07-K: Dispute Lifecycle Status Transitions")
    class LifecycleTransitionTests {

        @Test
        @DisplayName("OPEN -> UNDER_REVIEW (staff) -> RESOLVED (admin) transition sequence succeeds")
        void testTransition_openToUnderReviewToResolved() {
            Dispute dispute = new Dispute();
            dispute.setId(701L);
            dispute.setGroup(sampleGroup);
            dispute.setComplainantUser(complainant);
            dispute.setStatus(DisputeStatus.OPEN);

            when(disputeRepository.findById(701L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));
            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

            // Step 1: Staff mediates: OPEN -> UNDER_REVIEW
            DisputeResponse underReviewResp = disputeService.transitionDisputeStatus(
                    701L, DisputeStatus.UNDER_REVIEW, "Staff mediation initiated", null, 30L
            );
            assertEquals(DisputeStatus.UNDER_REVIEW, underReviewResp.getStatus());

            // Step 2: Admin final arbitration: UNDER_REVIEW -> RESOLVED
            DisputeResponse resolvedResp = disputeService.transitionDisputeStatus(
                    701L, DisputeStatus.RESOLVED, "Settlement agreed", "Deposit reimbursed", 60L
            );
            assertEquals(DisputeStatus.RESOLVED, resolvedResp.getStatus());
            assertEquals("Deposit reimbursed", resolvedResp.getResolutionSummary());
        }

        @Test
        @DisplayName("STAFF attempting to perform final arbitration (transition to RESOLVED) is rejected with HTTP 403 Forbidden")
        void testTransition_staffAttemptingResolved_rejectedWithForbidden() {
            Dispute dispute = new Dispute();
            dispute.setId(703L);
            dispute.setStatus(DisputeStatus.UNDER_REVIEW);

            when(disputeRepository.findById(703L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.transitionDisputeStatus(703L, DisputeStatus.RESOLVED, "Staff resolution", "Summary", 30L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("Staff members cannot perform final binding dispute arbitration"));
        }

        @Test
        @DisplayName("Transitioning from RESOLVED throws exception (terminal state)")
        void testTransition_fromResolved_throwsException() {
            Dispute dispute = new Dispute();
            dispute.setId(702L);
            dispute.setStatus(DisputeStatus.RESOLVED);

            when(disputeRepository.findById(702L)).thenReturn(Optional.of(dispute));
            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));

            assertThrows(BusinessException.class,
                    () -> disputeService.transitionDisputeStatus(702L, DisputeStatus.OPEN, "Reopen", null, 60L));
        }
    }

    @Nested
    @DisplayName("07-L: Dispute Evidence Attachment, Access Control & Audit Tests")
    class DisputeEvidenceTests {

        private Dispute sampleDispute;

        @BeforeEach
        void setUpDispute() {
            sampleDispute = new Dispute();
            sampleDispute.setId(800L);
            sampleDispute.setGroup(sampleGroup);
            sampleDispute.setComplainantUser(complainant);
            sampleDispute.setRespondentUser(respondent);
            sampleDispute.setStatus(DisputeStatus.OPEN);
        }

        @Test
        @DisplayName("Complainant attaches evidence successfully and audit log is recorded")
        void testAddEvidence_byComplainant_success() {
            CreateDisputeEvidenceRequest req = new CreateDisputeEvidenceRequest(
                    "https://storage.evshare.io/evidences/damage_closeup.png",
                    "{\"x\": 0.2, \"y\": 1.1, \"z\": 0.4}",
                    "Close-up photo of bumper dent"
            );

            when(disputeRepository.findById(800L)).thenReturn(Optional.of(sampleDispute));
            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));
            when(disputeEvidenceRepository.saveAndFlush(any(DisputeEvidence.class))).thenAnswer(inv -> {
                DisputeEvidence de = inv.getArgument(0);
                de.setId(801L);
                return de;
            });

            DisputeEvidenceResponse response = disputeService.addEvidence(800L, req, 10L);

            assertNotNull(response);
            assertEquals(801L, response.getId());
            assertEquals("https://storage.evshare.io/evidences/damage_closeup.png", response.getFileUrl());
            assertNotNull(response.getCreatedAt());

            // Verify audit log recorded
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, atLeastOnce()).save(auditCaptor.capture());
            AuditLog savedLog = auditCaptor.getValue();
            assertEquals("DISPUTE_EVIDENCE_ATTACHED", savedLog.getAction());
            assertEquals("DisputeEvidence", savedLog.getEntityName());
            assertEquals(801L, savedLog.getEntityId());
            assertEquals(10L, savedLog.getUser().getId());
            assertTrue(savedLog.getNewStateJson().contains("damage_closeup.png"));
        }

        @Test
        @DisplayName("Respondent can attach rebuttal evidence successfully")
        void testAddEvidence_byRespondent_success() {
            CreateDisputeEvidenceRequest req = new CreateDisputeEvidenceRequest(
                    "https://storage.evshare.io/evidences/precheck_photo.jpg",
                    null,
                    "Pre-check timestamped photo showing pre-existing scratch"
            );

            when(disputeRepository.findById(800L)).thenReturn(Optional.of(sampleDispute));
            when(userRepository.findById(20L)).thenReturn(Optional.of(respondent));
            when(disputeEvidenceRepository.saveAndFlush(any(DisputeEvidence.class))).thenAnswer(inv -> {
                DisputeEvidence de = inv.getArgument(0);
                de.setId(802L);
                return de;
            });

            DisputeEvidenceResponse response = disputeService.addEvidence(800L, req, 20L);

            assertNotNull(response);
            assertEquals(802L, response.getId());
            verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Active syndicate co-owner can attach witness evidence")
        void testAddEvidence_byActiveCoOwner_success() {
            User thirdOwner = new User();
            thirdOwner.setId(40L);
            thirdOwner.setEmail("charlie@evshare.io");
            thirdOwner.setIsActive(true);

            OwnershipShare thirdShare = createShare(3L, sampleGroup, thirdOwner, new BigDecimal("10.00"), true);

            CreateDisputeEvidenceRequest req = new CreateDisputeEvidenceRequest(
                    "https://storage.evshare.io/evidences/dashcam.mp4",
                    null,
                    "Dashcam footage during handover"
            );

            when(disputeRepository.findById(800L)).thenReturn(Optional.of(sampleDispute));
            when(userRepository.findById(40L)).thenReturn(Optional.of(thirdOwner));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 40L)).thenReturn(Optional.of(thirdShare));
            when(disputeEvidenceRepository.saveAndFlush(any(DisputeEvidence.class))).thenAnswer(inv -> {
                DisputeEvidence de = inv.getArgument(0);
                de.setId(803L);
                return de;
            });

            DisputeEvidenceResponse response = disputeService.addEvidence(800L, req, 40L);
            assertNotNull(response);
            assertEquals(803L, response.getId());
        }

        @Test
        @DisplayName("Outsider user is rejected with HTTP 403 Forbidden")
        void testAddEvidence_byOutsider_rejected() {
            User outsider = new User();
            outsider.setId(99L);
            outsider.setEmail("stranger@other.io");
            outsider.setIsActive(true);

            CreateDisputeEvidenceRequest req = new CreateDisputeEvidenceRequest(
                    "https://storage.evshare.io/evidences/fake.jpg", null, "Fake"
            );

            when(disputeRepository.findById(800L)).thenReturn(Optional.of(sampleDispute));
            when(userRepository.findById(99L)).thenReturn(Optional.of(outsider));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.addEvidence(800L, req, 99L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("not authorized to attach evidence"));
        }

        @Test
        @DisplayName("Inactive co-owner (share isActive = false) is rejected with HTTP 403 Forbidden")
        void testAddEvidence_byInactiveCoOwner_rejected() {
            User inactiveUser = new User();
            inactiveUser.setId(50L);
            inactiveUser.setEmail("inactive@evshare.io");
            inactiveUser.setIsActive(true);

            OwnershipShare inactiveShare = createShare(4L, sampleGroup, inactiveUser, new BigDecimal("10.00"), false);

            CreateDisputeEvidenceRequest req = new CreateDisputeEvidenceRequest(
                    "https://storage.evshare.io/evidences/note.pdf", null, "Note"
            );

            when(disputeRepository.findById(800L)).thenReturn(Optional.of(sampleDispute));
            when(userRepository.findById(50L)).thenReturn(Optional.of(inactiveUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 50L)).thenReturn(Optional.of(inactiveShare));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.addEvidence(800L, req, 50L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("not authorized to attach evidence"));
        }

        @Test
        @DisplayName("Attaching evidence to a RESOLVED dispute is rejected with HTTP 409 Conflict")
        void testAddEvidence_toResolvedDispute_rejected() {
            sampleDispute.setStatus(DisputeStatus.RESOLVED);

            CreateDisputeEvidenceRequest req = new CreateDisputeEvidenceRequest(
                    "https://storage.evshare.io/evidences/late_doc.pdf", null, "Late document"
            );

            when(disputeRepository.findById(800L)).thenReturn(Optional.of(sampleDispute));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.addEvidence(800L, req, 10L));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("Cannot attach evidence to a RESOLVED dispute"));
        }

        @Test
        @DisplayName("getDisputeEvidenceById returns matching evidence details")
        void testGetDisputeEvidenceById_success() {
            DisputeEvidence ev = new DisputeEvidence();
            ev.setId(850L);
            ev.setDispute(sampleDispute);
            ev.setUploadedByUser(complainant);
            ev.setFileUrl("https://storage.evshare.io/evidences/photo.jpg");
            ev.setDescription("Inspection photo");
            ev.setCreatedAt(Instant.now());

            when(disputeRepository.existsById(800L)).thenReturn(true);
            when(disputeEvidenceRepository.findById(850L)).thenReturn(Optional.of(ev));

            DisputeEvidenceResponse resp = disputeService.getDisputeEvidenceById(800L, 850L);
            assertNotNull(resp);
            assertEquals(850L, resp.getId());
            assertEquals("https://storage.evshare.io/evidences/photo.jpg", resp.getFileUrl());
        }

        @Test
        @DisplayName("getDisputeEvidenceById throws ResourceNotFoundException when evidence belongs to another dispute")
        void testGetDisputeEvidenceById_mismatchedDispute_throwsNotFound() {
            Dispute otherDispute = new Dispute();
            otherDispute.setId(999L);

            DisputeEvidence ev = new DisputeEvidence();
            ev.setId(850L);
            ev.setDispute(otherDispute);

            when(disputeRepository.existsById(800L)).thenReturn(true);
            when(disputeEvidenceRepository.findById(850L)).thenReturn(Optional.of(ev));

            assertThrows(ResourceNotFoundException.class,
                    () -> disputeService.getDisputeEvidenceById(800L, 850L));
        }

        @Test
        @DisplayName("getDisputeHistory returns chronological audit trail")
        void testGetDisputeHistory_success() {
            DisputeEvidence ev1 = new DisputeEvidence();
            ev1.setId(801L);
            ev1.setDispute(sampleDispute);

            AuditLog logDispute = new AuditLog();
            logDispute.setId(101L);
            logDispute.setEntityName("Dispute");
            logDispute.setEntityId(800L);
            logDispute.setAction("DISPUTE_CREATED");
            logDispute.setUser(complainant);
            logDispute.setCreatedAt(Instant.parse("2026-09-10T01:00:00Z"));

            AuditLog logEvidence = new AuditLog();
            logEvidence.setId(102L);
            logEvidence.setEntityName("DisputeEvidence");
            logEvidence.setEntityId(801L);
            logEvidence.setAction("DISPUTE_EVIDENCE_ATTACHED");
            logEvidence.setUser(complainant);
            logEvidence.setCreatedAt(Instant.parse("2026-09-10T01:05:00Z"));

            when(disputeRepository.existsById(800L)).thenReturn(true);
            when(auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Dispute", 800L))
                    .thenReturn(List.of(logDispute));
            when(disputeEvidenceRepository.findByDisputeId(800L)).thenReturn(List.of(ev1));
            when(auditLogRepository.findByEntityNameAndEntityIdInOrderByCreatedAtDesc("DisputeEvidence", List.of(801L)))
                    .thenReturn(List.of(logEvidence));

            var history = disputeService.getDisputeHistory(800L);
            assertNotNull(history);
            assertEquals(2, history.size());
            // Most recent first: logEvidence (01:05) before logDispute (01:00)
            assertEquals("DISPUTE_EVIDENCE_ATTACHED", history.get(0).getAction());
            assertEquals("DISPUTE_CREATED", history.get(1).getAction());
        }
    }

    @Nested
    @DisplayName("07-M: Staff Mediation & Review Tests")
    class StaffMediationTests {

        private Dispute openDispute;

        @BeforeEach
        void setUpDispute() {
            openDispute = new Dispute();
            openDispute.setId(900L);
            openDispute.setGroup(sampleGroup);
            openDispute.setComplainantUser(complainant);
            openDispute.setRespondentUser(respondent);
            openDispute.setStatus(DisputeStatus.UNDER_REVIEW);
        }

        @Test
        @DisplayName("Staff member records mediation notes successfully")
        void testAddMediationNotes_byStaff_success() {
            AddMediationNotesRequest req = new AddMediationNotesRequest("Called both co-owners. Agreed bumper scratch occurred during check-out window.");

            when(disputeRepository.findById(900L)).thenReturn(Optional.of(openDispute));
            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));
            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse resp = disputeService.addMediationNotes(900L, req, 30L);

            assertNotNull(resp);
            assertEquals("Called both co-owners. Agreed bumper scratch occurred during check-out window.", resp.getMediationNotes());
            assertEquals(30L, resp.getMediatorUserId());

            verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Co-owner attempting to add mediation notes is rejected with HTTP 403 Forbidden")
        void testAddMediationNotes_byCoOwner_rejected() {
            AddMediationNotesRequest req = new AddMediationNotesRequest("Co-owner notes");

            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.addMediationNotes(900L, req, 10L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("Only staff or administrators"));
        }

        @Test
        @DisplayName("Adding mediation notes to a RESOLVED dispute is rejected with HTTP 409 Conflict")
        void testAddMediationNotes_onResolvedDispute_rejected() {
            openDispute.setStatus(DisputeStatus.RESOLVED);
            AddMediationNotesRequest req = new AddMediationNotesRequest("Late notes");

            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));
            when(disputeRepository.findById(900L)).thenReturn(Optional.of(openDispute));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.addMediationNotes(900L, req, 30L));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("Cannot add mediation notes to a RESOLVED dispute"));
        }

        @Test
        @DisplayName("Staff member formulates and proposes resolution recommendation successfully")
        void testProposeResolution_byStaff_success() {
            ProposeResolutionRequest req = new ProposeResolutionRequest("Recommend 300,000 VND reimbursement from respondent shared balance.");

            when(disputeRepository.findById(900L)).thenReturn(Optional.of(openDispute));
            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));
            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse resp = disputeService.proposeResolution(900L, req, 30L);

            assertNotNull(resp);
            assertEquals("Recommend 300,000 VND reimbursement from respondent shared balance.", resp.getProposedResolution());
            assertEquals(30L, resp.getMediatorUserId());

            verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Co-owner attempting to propose resolution is rejected with HTTP 403 Forbidden")
        void testProposeResolution_byCoOwner_rejected() {
            ProposeResolutionRequest req = new ProposeResolutionRequest("My terms");

            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.proposeResolution(900L, req, 10L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("Only staff or administrators"));
        }

        @Test
        @DisplayName("Proposing resolution for a RESOLVED dispute is rejected with HTTP 409 Conflict")
        void testProposeResolution_onResolvedDispute_rejected() {
            openDispute.setStatus(DisputeStatus.RESOLVED);
            ProposeResolutionRequest req = new ProposeResolutionRequest("Late terms");

            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));
            when(disputeRepository.findById(900L)).thenReturn(Optional.of(openDispute));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.proposeResolution(900L, req, 30L));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("Cannot propose resolution for a RESOLVED dispute"));
        }

        @Test
        @DisplayName("getDisputesForStaffReview retrieves list of active pending disputes")
        void testGetDisputesForStaffReview_success() {
            when(disputeRepository.findByStatusIn(any()))
                    .thenReturn(List.of(openDispute));

            List<DisputeResponse> list = disputeService.getDisputesForStaffReview(null);
            assertNotNull(list);
            assertEquals(1, list.size());
            assertEquals(900L, list.get(0).getId());
        }
    }

    @Nested
    @DisplayName("07-N: Admin Final Arbitration Tests (BR-DIS-05)")
    class AdminArbitrationTests {

        private Dispute openDispute;
        private DisputeEvidence sampleEvidence;

        @BeforeEach
        void setUpDispute() {
            openDispute = new Dispute();
            openDispute.setId(950L);
            openDispute.setGroup(sampleGroup);
            openDispute.setComplainantUser(complainant);
            openDispute.setRespondentUser(respondent);
            openDispute.setTitle("Front bumper collision scratch");
            openDispute.setDescription("Vehicle returned with scratch on front bumper");
            openDispute.setStatus(DisputeStatus.OPEN);

            sampleEvidence = new DisputeEvidence();
            sampleEvidence.setId(951L);
            sampleEvidence.setDispute(openDispute);
            sampleEvidence.setUploadedByUser(complainant);
            sampleEvidence.setFileUrl("https://evidence.evshare.io/scratch.jpg");
            sampleEvidence.setCreatedAt(Instant.now());
        }

        @Test
        @DisplayName("Admin successfully arbitrates dispute from OPEN to RESOLVED")
        void testArbitrateDispute_byAdmin_fromOpen_success() {
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "Exhaustive telemetry and 3D defect coordinate inspection confirms respondent fault.",
                    "Respondent is assessed 350,000 VND maintenance deductible. Dispute closed as RESOLVED."
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(950L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(950L)).thenReturn(List.of(sampleEvidence));
            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse resp = disputeService.arbitrateDispute(950L, req, 60L);

            assertNotNull(resp);
            assertEquals(DisputeStatus.RESOLVED, resp.getStatus());
            assertEquals(req.getResolutionSummary(), resp.getResolutionSummary());
            assertEquals(60L, resp.getArbitratorUserId());
            assertEquals("Admin Arbitrator", resp.getArbitratorUserName());
            assertNotNull(resp.getResolvedAt());

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, atLeastOnce()).save(captor.capture());
            AuditLog savedLog = captor.getValue();
            assertEquals("DISPUTE_ARBITRATED", savedLog.getAction());
            assertEquals("Dispute", savedLog.getEntityName());
            assertEquals(950L, savedLog.getEntityId());
        }

        @Test
        @DisplayName("Admin successfully arbitrates dispute from UNDER_REVIEW to RESOLVED")
        void testArbitrateDispute_byAdmin_fromUnderReview_success() {
            openDispute.setStatus(DisputeStatus.UNDER_REVIEW);
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "Staff mediation deadlocked; admin ruling issued based on dashcam metadata.",
                    "Both parties split repair 50/50 from group reserve."
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(950L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(950L)).thenReturn(List.of(sampleEvidence));
            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse resp = disputeService.arbitrateDispute(950L, req, 60L);

            assertNotNull(resp);
            assertEquals(DisputeStatus.RESOLVED, resp.getStatus());
            assertEquals(60L, resp.getArbitratorUserId());
            assertNotNull(resp.getResolvedAt());
        }

        @Test
        @DisplayName("Admin successfully arbitrates dispute from ESCALATED to RESOLVED")
        void testArbitrateDispute_byAdmin_fromEscalated_success() {
            openDispute.setStatus(DisputeStatus.ESCALATED);
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "Escalated dispute reviewed with full vehicle service records.",
                    "Full compensation of 1,200,000 VND ordered to complainant."
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(950L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(950L)).thenReturn(List.of(sampleEvidence));
            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(inv -> inv.getArgument(0));

            DisputeResponse resp = disputeService.arbitrateDispute(950L, req, 60L);

            assertNotNull(resp);
            assertEquals(DisputeStatus.RESOLVED, resp.getStatus());
            assertEquals(60L, resp.getArbitratorUserId());
        }

        @Test
        @DisplayName("Staff member attempting final arbitration is rejected with HTTP 403 Forbidden")
        void testArbitrateDispute_byStaff_rejectedWithForbidden() {
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "Staff trying to arbitrate", "Staff resolution"
            );

            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.arbitrateDispute(950L, req, 30L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("Only platform administrators can perform final binding dispute arbitration"));
        }

        @Test
        @DisplayName("Syndicate co-owner attempting final arbitration is rejected with HTTP 403 Forbidden")
        void testArbitrateDispute_byCoOwner_rejectedWithForbidden() {
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "Owner trying to arbitrate", "Owner ruling"
            );

            when(userRepository.findById(10L)).thenReturn(Optional.of(complainant));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.arbitrateDispute(950L, req, 10L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("Only platform administrators can perform final binding dispute arbitration"));
        }

        @Test
        @DisplayName("Arbitration with blank reason is rejected with HTTP 400 Bad Request")
        void testArbitrateDispute_blankReason_rejected() {
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "   ", "Valid resolution summary"
            );

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.arbitrateDispute(950L, req, 60L));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("Arbitration reason cannot be blank"));
        }

        @Test
        @DisplayName("Arbitration with blank resolution summary is rejected with HTTP 400 Bad Request")
        void testArbitrateDispute_blankResolutionSummary_rejected() {
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "Valid reason for decision", "   "
            );

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.arbitrateDispute(950L, req, 60L));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("Resolution summary cannot be blank"));
        }

        @Test
        @DisplayName("Arbitrating an already RESOLVED dispute is rejected with HTTP 409 Conflict")
        void testArbitrateDispute_alreadyResolved_rejectedWithConflict() {
            openDispute.setStatus(DisputeStatus.RESOLVED);
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "New review attempt", "New resolution terms"
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(950L)).thenReturn(Optional.of(openDispute));

            assertThrows(InvalidDisputeStateTransitionException.class,
                    () -> disputeService.arbitrateDispute(950L, req, 60L));
        }

        @Test
        @DisplayName("Arbitrating dispute lacking evidence records is rejected with HTTP 400 Bad Request")
        void testArbitrateDispute_noEvidence_rejected() {
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "Valid arbitration reasoning", "Valid resolution summary"
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(950L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(950L)).thenReturn(List.of());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.arbitrateDispute(950L, req, 60L));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("cannot be arbitrated without evidence records"));
        }

        @Test
        @DisplayName("getArbitrationDossier returns full dossier for admin evidence review")
        void testGetArbitrationDossier_byAdmin_success() {
            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(950L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(950L)).thenReturn(List.of(sampleEvidence));
            when(disputeRepository.existsById(950L)).thenReturn(true);
            when(auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Dispute", 950L)).thenReturn(List.of());
            when(auditLogRepository.findByEntityNameAndEntityIdInOrderByCreatedAtDesc("DisputeEvidence", List.of(951L))).thenReturn(List.of());

            DisputeArbitrationDossierResponse dossier = disputeService.getArbitrationDossier(950L, 60L);

            assertNotNull(dossier);
            assertEquals(950L, dossier.getDispute().getId());
            assertEquals(1, dossier.getTotalEvidences());
            assertEquals(1, dossier.getEvidences().size());
            assertNotNull(dossier.getHistory());
        }

        @Test
        @DisplayName("getArbitrationDossier by non-admin is rejected with HTTP 403 Forbidden")
        void testGetArbitrationDossier_byNonAdmin_rejected() {
            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.getArbitrationDossier(950L, 30L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("Only platform administrators can access the arbitration dossier"));
        }
    }

    @Nested
    @DisplayName("07-O: Dispute Fund Adjustment Tests (BR-DIS-06)")
    class DisputeFundAdjustmentTests {

        private Dispute openDispute;
        private DisputeEvidence sampleEvidence;

        @BeforeEach
        void setUpDispute() {
            openDispute = new Dispute();
            openDispute.setId(960L);
            openDispute.setGroup(sampleGroup);
            openDispute.setComplainantUser(complainant);
            openDispute.setRespondentUser(respondent);
            openDispute.setStatus(DisputeStatus.UNDER_REVIEW);
            openDispute.setTitle("Front bumper scratch dispute");
            openDispute.setDescription("Unresolved damage after joint inspection");

            sampleEvidence = new DisputeEvidence();
            sampleEvidence.setId(961L);
            sampleEvidence.setDispute(openDispute);
            sampleEvidence.setUploadedByUser(complainant);
            sampleEvidence.setFileUrl("https://storage.evshare.io/evidences/scratch_dent.jpg");
            sampleEvidence.setCreatedAt(Instant.now());
        }

        @Test
        @DisplayName("Admin executes dispute arbitration with SharedFund DEBIT adjustment successfully")
        void testArbitrateDisputeWithFundAdjustment_debit_success() {
            DisputeFundAdjustmentRequest req = new DisputeFundAdjustmentRequest(
                    "Reimbursement for bumper repair per inspection",
                    "Award 500,000 VND repair reimbursement from syndicate SharedFund",
                    new BigDecimal("500000.00"),
                    TransactionEntryType.DEBIT
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(960L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(960L)).thenReturn(List.of(sampleEvidence));
            when(sharedFundRepository.findByGroupIdWithLock(1L)).thenReturn(Optional.of(sampleSharedFund));
            when(sharedFundRepository.save(any(SharedFund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            FundTransaction savedTx = new FundTransaction();
            savedTx.setId(777L);
            savedTx.setAmount(new BigDecimal("500000.00"));
            savedTx.setBalanceAfter(new BigDecimal("4500000.00"));
            savedTx.setTransactionReference("DISP-960-A1B2C3D4");
            when(fundTransactionRepository.save(any(FundTransaction.class))).thenReturn(savedTx);

            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DisputeResponse response = disputeService.arbitrateDisputeWithFundAdjustment(960L, req, 60L);

            assertNotNull(response);
            assertEquals(DisputeStatus.RESOLVED, response.getStatus());
            assertEquals(new BigDecimal("500000.00"), response.getFundAdjustmentAmount());
            assertEquals(777L, response.getFundTransactionId());
            assertEquals("DISP-960-A1B2C3D4", response.getFundTransactionReference());
            assertEquals(new BigDecimal("4500000.00"), sampleSharedFund.getCurrentBalance());

            // Verify fund transaction creation
            ArgumentCaptor<FundTransaction> txCaptor = ArgumentCaptor.forClass(FundTransaction.class);
            verify(fundTransactionRepository).save(txCaptor.capture());
            FundTransaction capturedTx = txCaptor.getValue();
            assertEquals(TransactionType.DISPUTE_ADJUSTMENT, capturedTx.getTransactionType());
            assertEquals(FundTransactionSource.DISPUTE_RESOLUTION, capturedTx.getSource());
            assertEquals(TransactionEntryType.DEBIT, capturedTx.getEntryType());
            assertEquals(new BigDecimal("500000.00"), capturedTx.getAmount());
            assertEquals(new BigDecimal("4500000.00"), capturedTx.getBalanceAfter());

            // Verify dual audit logs (DISPUTE_ARBITRATED & SHARED_FUND_DISPUTE_ADJUSTMENT)
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository, times(2)).save(auditCaptor.capture());
            List<AuditLog> auditLogs = auditCaptor.getAllValues();
            assertTrue(auditLogs.stream().anyMatch(a -> "DISPUTE_ARBITRATED".equals(a.getAction())));
            assertTrue(auditLogs.stream().anyMatch(a -> "SHARED_FUND_DISPUTE_ADJUSTMENT".equals(a.getAction())));
        }

        @Test
        @DisplayName("Admin executes dispute arbitration with SharedFund CREDIT adjustment successfully")
        void testArbitrateDisputeWithFundAdjustment_credit_success() {
            DisputeFundAdjustmentRequest req = new DisputeFundAdjustmentRequest(
                    "Penalty deduction recovery",
                    "Credit 300,000 VND penalty compensation into syndicate SharedFund",
                    new BigDecimal("300000.00"),
                    TransactionEntryType.CREDIT
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(960L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(960L)).thenReturn(List.of(sampleEvidence));
            when(sharedFundRepository.findByGroupIdWithLock(1L)).thenReturn(Optional.of(sampleSharedFund));
            when(sharedFundRepository.save(any(SharedFund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            FundTransaction savedTx = new FundTransaction();
            savedTx.setId(778L);
            savedTx.setAmount(new BigDecimal("300000.00"));
            savedTx.setBalanceAfter(new BigDecimal("5300000.00"));
            savedTx.setTransactionReference("DISP-960-CREDIT01");
            when(fundTransactionRepository.save(any(FundTransaction.class))).thenReturn(savedTx);

            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DisputeResponse response = disputeService.arbitrateDisputeWithFundAdjustment(960L, req, 60L);

            assertNotNull(response);
            assertEquals(DisputeStatus.RESOLVED, response.getStatus());
            assertEquals(new BigDecimal("5300000.00"), sampleSharedFund.getCurrentBalance());
        }

        @Test
        @DisplayName("DEBIT adjustment exceeding fund balance throws InsufficientFundBalanceException")
        void testArbitrateDisputeWithFundAdjustment_insufficientBalance_throwsException() {
            sampleSharedFund.setCurrentBalance(new BigDecimal("100000.00"));

            DisputeFundAdjustmentRequest req = new DisputeFundAdjustmentRequest(
                    "Large payout attempt",
                    "Attempting 500,000 VND reimbursement when balance is only 100,000 VND",
                    new BigDecimal("500000.00"),
                    TransactionEntryType.DEBIT
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(960L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(960L)).thenReturn(List.of(sampleEvidence));
            when(sharedFundRepository.findByGroupIdWithLock(1L)).thenReturn(Optional.of(sampleSharedFund));

            assertThrows(InsufficientFundBalanceException.class,
                    () -> disputeService.arbitrateDisputeWithFundAdjustment(960L, req, 60L));

            // Verify no transaction or audit log saved
            verify(fundTransactionRepository, never()).save(any());
            verify(auditLogRepository, never()).save(any());
            assertEquals(new BigDecimal("100000.00"), sampleSharedFund.getCurrentBalance());
            assertEquals(DisputeStatus.UNDER_REVIEW, openDispute.getStatus());
        }

        @Test
        @DisplayName("Duplicate resolution on already RESOLVED dispute is rejected with HTTP 409 Conflict")
        void testArbitrateDisputeWithFundAdjustment_alreadyResolved_rejectedWithConflict() {
            openDispute.setStatus(DisputeStatus.RESOLVED);

            DisputeFundAdjustmentRequest req = new DisputeFundAdjustmentRequest(
                    "Second arbitration attempt",
                    "Duplicate resolution terms",
                    new BigDecimal("200000.00"),
                    TransactionEntryType.DEBIT
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(960L)).thenReturn(Optional.of(openDispute));

            assertThrows(InvalidDisputeStateTransitionException.class,
                    () -> disputeService.arbitrateDisputeWithFundAdjustment(960L, req, 60L));

            verify(sharedFundRepository, never()).findByGroupIdWithLock(any());
            verify(fundTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Dispute that already has a fundTransaction cannot be adjusted again (HTTP 409 Conflict)")
        void testArbitrateDisputeWithFundAdjustment_alreadyHasFundTransaction_rejected() {
            FundTransaction existingTx = new FundTransaction();
            existingTx.setId(123L);
            openDispute.setFundTransaction(existingTx);

            DisputeFundAdjustmentRequest req = new DisputeFundAdjustmentRequest(
                    "Duplicate payout attempt",
                    "Resolution terms",
                    new BigDecimal("200000.00"),
                    TransactionEntryType.DEBIT
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(960L)).thenReturn(Optional.of(openDispute));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.arbitrateDisputeWithFundAdjustment(960L, req, 60L));
            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("already has an associated financial fund adjustment"));
        }

        @Test
        @DisplayName("Fund adjustment attempted by non-admin is rejected with HTTP 403 Forbidden")
        void testArbitrateDisputeWithFundAdjustment_nonAdmin_forbidden() {
            DisputeFundAdjustmentRequest req = new DisputeFundAdjustmentRequest(
                    "Staff arbitration attempt",
                    "Unauthorized resolution terms",
                    new BigDecimal("200000.00"),
                    TransactionEntryType.DEBIT
            );

            when(userRepository.findById(30L)).thenReturn(Optional.of(staffUser));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.arbitrateDisputeWithFundAdjustment(960L, req, 30L));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("Only platform administrators can perform dispute fund adjustments"));
        }

        @Test
        @DisplayName("Dispute without evidence cannot be arbitrated with fund adjustment (HTTP 400 Bad Request)")
        void testArbitrateDisputeWithFundAdjustment_noEvidence_rejected() {
            DisputeFundAdjustmentRequest req = new DisputeFundAdjustmentRequest(
                    "Valid reason",
                    "Valid summary",
                    new BigDecimal("200000.00"),
                    TransactionEntryType.DEBIT
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(960L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(960L)).thenReturn(List.of());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> disputeService.arbitrateDisputeWithFundAdjustment(960L, req, 60L));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("cannot be arbitrated without evidence records"));
        }

        @Test
        @DisplayName("arbitrateDispute delegates to fund adjustment when fund adjustment parameters are supplied")
        void testArbitrateDispute_withEmbeddedFundAdjustment_delegatesSuccessfully() {
            AdminArbitrateDisputeRequest req = new AdminArbitrateDisputeRequest(
                    "Valid arbitration reasoning",
                    "Valid resolution with 400,000 VND compensation",
                    new BigDecimal("400000.00"),
                    TransactionEntryType.DEBIT
            );

            when(userRepository.findById(60L)).thenReturn(Optional.of(adminUser));
            when(disputeRepository.findById(960L)).thenReturn(Optional.of(openDispute));
            when(disputeEvidenceRepository.findByDisputeId(960L)).thenReturn(List.of(sampleEvidence));
            when(sharedFundRepository.findByGroupIdWithLock(1L)).thenReturn(Optional.of(sampleSharedFund));
            when(sharedFundRepository.save(any(SharedFund.class))).thenAnswer(invocation -> invocation.getArgument(0));

            FundTransaction savedTx = new FundTransaction();
            savedTx.setId(888L);
            savedTx.setAmount(new BigDecimal("400000.00"));
            savedTx.setBalanceAfter(new BigDecimal("4600000.00"));
            savedTx.setTransactionReference("DISP-960-DELEGATED");
            when(fundTransactionRepository.save(any(FundTransaction.class))).thenReturn(savedTx);

            when(disputeRepository.saveAndFlush(any(Dispute.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DisputeResponse response = disputeService.arbitrateDispute(960L, req, 60L);

            assertNotNull(response);
            assertEquals(DisputeStatus.RESOLVED, response.getStatus());
            assertEquals(new BigDecimal("400000.00"), response.getFundAdjustmentAmount());
            assertEquals(888L, response.getFundTransactionId());
        }
    }
}
