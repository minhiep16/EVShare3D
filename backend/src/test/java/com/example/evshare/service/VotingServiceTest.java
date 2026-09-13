package com.example.evshare.service;

import com.example.evshare.dto.request.CastVoteRequest;
import com.example.evshare.dto.request.CreateProposalRequest;
import com.example.evshare.dto.response.ProposalResponse;
import com.example.evshare.dto.response.ProposalResultsResponse;
import com.example.evshare.dto.response.ProposalTallyResponse;
import com.example.evshare.dto.response.ProposerEligibilityResponse;
import com.example.evshare.dto.response.VoteResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.entity.enums.ProposalType;
import com.example.evshare.entity.enums.VoteOptionKey;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.exception.InsufficientEquityException;
import com.example.evshare.exception.ResourceNotFoundException;
import com.example.evshare.repository.*;
import com.example.evshare.service.impl.VotingServiceImpl;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VotingServiceTest {

    @Mock private ProposalRepository proposalRepository;
    @Mock private VoteOptionRepository voteOptionRepository;
    @Mock private OwnershipGroupRepository ownershipGroupRepository;
    @Mock private OwnershipShareRepository ownershipShareRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private VoteRepository voteRepository;

    private VotingServiceImpl votingService;

    private OwnershipGroup sampleGroup;
    private User sampleUser;
    private OwnershipShare sampleShare;

    @BeforeEach
    void setUp() {
        votingService = new VotingServiceImpl(
                proposalRepository,
                voteOptionRepository,
                ownershipGroupRepository,
                ownershipShareRepository,
                userRepository,
                auditLogRepository,
                new ProposalStateMachine(),
                voteRepository
        );

        sampleGroup = new OwnershipGroup(1L, "VinFast VF8 Syndicate Alpha", null, null, true);
        sampleUser = new User();
        sampleUser.setId(5L);
        sampleUser.setFullName("Nguyen Van A");
        sampleUser.setEmail("nguyen.a@evshare.io");
        sampleUser.setIsActive(true);

        sampleShare = new OwnershipShare();
        sampleShare.setId(10L);
        sampleShare.setGroup(sampleGroup);
        sampleShare.setUser(sampleUser);
        sampleShare.setPercentage(new BigDecimal("25.00"));
        sampleShare.setIsActive(true);
    }

    @Nested
    @DisplayName("Proposal Creation Tests")
    class ProposalCreationTests {

        @Test
        @DisplayName("Should successfully create ROUTINE_EXPENSE proposal with seeded vote options")
        void createProposal_routineExpense_success() {
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Tire Replacement & Wheel Alignment",
                    "Replacing front tires after 25,000 km of usage",
                    ProposalType.ROUTINE_EXPENSE,
                    null // test default 72h deadline
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> {
                Proposal p = inv.getArgument(0);
                p.setId(100L);
                return p;
            });

            when(voteOptionRepository.saveAll(anyList())).thenAnswer(inv -> {
                List<VoteOption> opts = inv.getArgument(0);
                long idGen = 1L;
                for (VoteOption opt : opts) {
                    opt.setId(idGen++);
                }
                return opts;
            });

            ProposalResponse response = votingService.createProposal(request, 5L);

            assertNotNull(response);
            assertEquals(100L, response.getId());
            assertEquals(1L, response.getGroupId());
            assertEquals(5L, response.getProposerUserId());
            assertEquals("Tire Replacement & Wheel Alignment", response.getTitle());
            assertEquals(ProposalType.ROUTINE_EXPENSE, response.getProposalType());
            assertEquals(ProposalStatus.ACTIVE, response.getStatus());
            assertNotNull(response.getVotingDeadline());
            assertTrue(response.getVotingDeadline().isAfter(Instant.now().plus(70, ChronoUnit.HOURS)));

            assertEquals(3, response.getOptions().size());
            assertTrue(response.getOptions().stream().anyMatch(o -> o.getOptionKey() == VoteOptionKey.APPROVE));
            assertTrue(response.getOptions().stream().anyMatch(o -> o.getOptionKey() == VoteOptionKey.REJECT));
            assertTrue(response.getOptions().stream().anyMatch(o -> o.getOptionKey() == VoteOptionKey.ABSTAIN));

            // Verify Audit Log captured
            ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(auditCaptor.capture());
            assertEquals("PROPOSAL_CREATED", auditCaptor.getValue().getAction());
            assertEquals("Proposal", auditCaptor.getValue().getEntityName());
            assertEquals(100L, auditCaptor.getValue().getEntityId());
        }

        @Test
        @DisplayName("Should successfully create MAJOR_EXPENSE proposal with custom deadline")
        void createProposal_majorExpense_withCustomDeadline() {
            Instant customDeadline = Instant.now().plus(48, ChronoUnit.HOURS);
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Traction Battery Replacement Upgrade",
                    "Upgrade to higher capacity battery module",
                    ProposalType.MAJOR_EXPENSE,
                    customDeadline
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> {
                Proposal p = inv.getArgument(0);
                p.setId(101L);
                return p;
            });
            when(voteOptionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            ProposalResponse response = votingService.createProposal(request, 5L);

            assertNotNull(response);
            assertEquals(101L, response.getId());
            assertEquals(ProposalType.MAJOR_EXPENSE, response.getProposalType());
            assertEquals(customDeadline, response.getVotingDeadline());
        }

        @Test
        @DisplayName("Should create OPERATIONAL_RULE_CHANGE proposal successfully")
        void createProposal_operationalRuleChange() {
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Modify Turnaround Buffer to 45 Minutes",
                    "Extend buffer period between bookings for thorough cleaning",
                    ProposalType.OPERATIONAL_RULE_CHANGE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> {
                Proposal p = inv.getArgument(0);
                p.setId(102L);
                return p;
            });
            when(voteOptionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            ProposalResponse response = votingService.createProposal(request, 5L);
            assertEquals(ProposalType.OPERATIONAL_RULE_CHANGE, response.getProposalType());
        }

        @Test
        @DisplayName("Should create OWNER_ADMISSION_OR_EXIT proposal successfully")
        void createProposal_ownerAdmissionOrExit() {
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Admit Member Tran Van B with 10% Equity",
                    "Transfer 10% from existing treasury pool to new co-owner",
                    ProposalType.OWNER_ADMISSION_OR_EXIT,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> {
                Proposal p = inv.getArgument(0);
                p.setId(103L);
                return p;
            });
            when(voteOptionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            ProposalResponse response = votingService.createProposal(request, 5L);
            assertEquals(ProposalType.OWNER_ADMISSION_OR_EXIT, response.getProposalType());
        }

        @Test
        @DisplayName("Should reject proposal when active equity stake is strictly below 10.00% (BR-VOT-01)")
        void createProposal_throwsInsufficientEquityException_whenEquityBelow10Percent() {
            sampleShare.setPercentage(new BigDecimal("9.99"));

            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Proposal with insufficient stake",
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            InsufficientEquityException ex = assertThrows(InsufficientEquityException.class, () ->
                    votingService.createProposal(request, 5L));

            assertTrue(ex.getMessage().contains("at least 10.00%"));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(proposalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should permit proposal when active equity stake is exactly 10.00%")
        void createProposal_permits_whenEquityExactly10Percent() {
            sampleShare.setPercentage(new BigDecimal("10.00"));

            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Proposal with exact 10% boundary",
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> {
                Proposal p = inv.getArgument(0);
                p.setId(104L);
                return p;
            });
            when(voteOptionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> votingService.createProposal(request, 5L));
        }

        @Test
        @DisplayName("Should reject proposal when proposer is not a co-owner in the target group")
        void createProposal_throwsForbidden_whenNotMemberOfGroup() {
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Unauthorized Proposal",
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    votingService.createProposal(request, 5L));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("not an active co-owner"));
        }

        @Test
        @DisplayName("Should reject proposal when ownership group is not found")
        void createProposal_throwsNotFound_whenGroupMissing() {
            CreateProposalRequest request = new CreateProposalRequest(
                    999L,
                    "Non-existent group",
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    votingService.createProposal(request, 5L));
        }

        @Test
        @DisplayName("Should reject proposal when ownership group is inactive")
        void createProposal_throwsBusinessException_whenGroupInactive() {
            sampleGroup.setIsActive(false);

            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Inactive group proposal",
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    votingService.createProposal(request, 5L));

            assertTrue(ex.getMessage().contains("inactive"));
        }

        @Test
        @DisplayName("Should reject proposal when proposer account is inactive")
        void createProposal_throwsBusinessException_whenUserInactive() {
            sampleUser.setIsActive(false);

            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Inactive user proposal",
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    votingService.createProposal(request, 5L));

            assertTrue(ex.getMessage().contains("inactive"));
        }

        @Test
        @DisplayName("Should reject proposal when category is missing")
        void createProposal_throwsBusinessException_whenCategoryNull() {
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Missing category",
                    "Rationale",
                    null,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    votingService.createProposal(request, 5L));

            assertTrue(ex.getMessage().contains("mandatory"));
        }

        @Test
        @DisplayName("Should reject proposal when title is blank")
        void createProposal_throwsBusinessException_whenTitleBlank() {
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "   ",
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    votingService.createProposal(request, 5L));

            assertTrue(ex.getMessage().contains("title cannot be blank"));
        }

        @Test
        @DisplayName("Should reject proposal when title exceeds 150 characters")
        void createProposal_throwsBusinessException_whenTitleTooLong() {
            String longTitle = "A".repeat(151);
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    longTitle,
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    votingService.createProposal(request, 5L));

            assertTrue(ex.getMessage().contains("cannot exceed 150 characters"));
        }

        @Test
        @DisplayName("Should reject proposal when description is blank")
        void createProposal_throwsBusinessException_whenDescriptionBlank() {
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Valid Title",
                    "",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    votingService.createProposal(request, 5L));

            assertTrue(ex.getMessage().contains("description cannot be blank"));
        }

        @Test
        @DisplayName("Should reject proposal when voting deadline is in the past")
        void createProposal_throwsBusinessException_whenDeadlineInPast() {
            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Valid Title",
                    "Valid Description",
                    ProposalType.ROUTINE_EXPENSE,
                    Instant.now().minus(1, ChronoUnit.HOURS)
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));

            BusinessException ex = assertThrows(BusinessException.class, () ->
                    votingService.createProposal(request, 5L));

            assertTrue(ex.getMessage().contains("strictly in the future"));
        }
    }

    @Nested
    @DisplayName("Proposal Query Tests")
    class ProposalQueryTests {

        @Test
        @DisplayName("Should get proposal by ID with options")
        void getProposalById_success() {
            Proposal proposal = new Proposal();
            proposal.setId(10L);
            proposal.setGroup(sampleGroup);
            proposal.setProposerUser(sampleUser);
            proposal.setTitle("Test Proposal");
            proposal.setDescription("Test Description");
            proposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
            proposal.setStatus(ProposalStatus.ACTIVE);
            proposal.setVotingDeadline(Instant.now().plus(72, ChronoUnit.HOURS));

            VoteOption opt = new VoteOption(1L, proposal, VoteOptionKey.APPROVE, "Approve");

            when(proposalRepository.findById(10L)).thenReturn(Optional.of(proposal));
            when(voteOptionRepository.findByProposalId(10L)).thenReturn(List.of(opt));

            ProposalResponse response = votingService.getProposalById(10L);

            assertNotNull(response);
            assertEquals(10L, response.getId());
            assertEquals("Test Proposal", response.getTitle());
            assertEquals(1, response.getOptions().size());
            assertEquals(VoteOptionKey.APPROVE, response.getOptions().get(0).getOptionKey());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when proposal does not exist")
        void getProposalById_notFound() {
            when(proposalRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    votingService.getProposalById(999L));
        }

        @Test
        @DisplayName("Should get proposals by group ID")
        void getProposalsByGroupId_success() {
            Proposal p1 = new Proposal();
            p1.setId(10L);
            p1.setGroup(sampleGroup);
            p1.setProposerUser(sampleUser);
            p1.setTitle("P1");
            p1.setDescription("D1");
            p1.setProposalType(ProposalType.ROUTINE_EXPENSE);
            p1.setStatus(ProposalStatus.ACTIVE);

            when(ownershipGroupRepository.existsById(1L)).thenReturn(true);
            when(proposalRepository.findByGroupId(1L)).thenReturn(List.of(p1));
            when(voteOptionRepository.findByProposalId(10L)).thenReturn(List.of());

            List<ProposalResponse> responses = votingService.getProposalsByGroupId(1L, null);

            assertEquals(1, responses.size());
            assertEquals(10L, responses.get(0).getId());
        }

        @Test
        @DisplayName("Should filter proposals by status")
        void getProposalsByGroupId_withStatus() {
            Proposal p1 = new Proposal();
            p1.setId(10L);
            p1.setGroup(sampleGroup);
            p1.setProposerUser(sampleUser);
            p1.setTitle("P1");
            p1.setDescription("D1");
            p1.setProposalType(ProposalType.ROUTINE_EXPENSE);
            p1.setStatus(ProposalStatus.ACTIVE);

            when(ownershipGroupRepository.existsById(1L)).thenReturn(true);
            when(proposalRepository.findByGroupIdAndStatus(1L, ProposalStatus.ACTIVE)).thenReturn(List.of(p1));
            when(voteOptionRepository.findByProposalId(10L)).thenReturn(List.of());

            List<ProposalResponse> responses = votingService.getProposalsByGroupId(1L, ProposalStatus.ACTIVE);

            assertEquals(1, responses.size());
            assertEquals(ProposalStatus.ACTIVE, responses.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("Proposal Lifecycle & History Tests")
    class ProposalLifecycleTests {

        @Test
        @DisplayName("Should successfully transition proposal from ACTIVE to PASSED and preserve audit trail")
        void transition_activeToPassed_success() {
            Proposal proposal = new Proposal();
            proposal.setId(20L);
            proposal.setGroup(sampleGroup);
            proposal.setProposerUser(sampleUser);
            proposal.setTitle("Passed Proposal");
            proposal.setDescription("Description");
            proposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
            proposal.setStatus(ProposalStatus.ACTIVE);
            proposal.setVotingDeadline(Instant.now().plus(72, ChronoUnit.HOURS));

            when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
            when(voteOptionRepository.findByProposalId(20L)).thenReturn(List.of());

            ProposalResponse response = votingService.transitionProposalStatus(
                    20L, ProposalStatus.PASSED, "Quorum and approval threshold satisfied", 5L
            );

            assertNotNull(response);
            assertEquals(ProposalStatus.PASSED, response.getStatus());
            assertEquals(ProposalStatus.PASSED, proposal.getStatus());

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());
            assertEquals("PROPOSAL_STATE_TRANSITION", captor.getValue().getAction());
            assertEquals(20L, captor.getValue().getEntityId());
            assertEquals("Proposal", captor.getValue().getEntityName());
            assertTrue(captor.getValue().getOldStateJson().contains("ACTIVE"));
            assertTrue(captor.getValue().getNewStateJson().contains("PASSED"));
        }

        @Test
        @DisplayName("Should successfully transition proposal from ACTIVE to REJECTED")
        void transition_activeToRejected_success() {
            Proposal proposal = new Proposal();
            proposal.setId(21L);
            proposal.setGroup(sampleGroup);
            proposal.setProposerUser(sampleUser);
            proposal.setStatus(ProposalStatus.ACTIVE);

            when(proposalRepository.findById(21L)).thenReturn(Optional.of(proposal));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
            when(voteOptionRepository.findByProposalId(21L)).thenReturn(List.of());

            ProposalResponse response = votingService.transitionProposalStatus(
                    21L, ProposalStatus.REJECTED, "Supermajority threshold failed", 5L
            );

            assertEquals(ProposalStatus.REJECTED, response.getStatus());
        }

        @Test
        @DisplayName("Should successfully transition proposal from ACTIVE to EXPIRED")
        void transition_activeToExpired_success() {
            Proposal proposal = new Proposal();
            proposal.setId(22L);
            proposal.setGroup(sampleGroup);
            proposal.setProposerUser(sampleUser);
            proposal.setStatus(ProposalStatus.ACTIVE);

            when(proposalRepository.findById(22L)).thenReturn(Optional.of(proposal));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
            when(voteOptionRepository.findByProposalId(22L)).thenReturn(List.of());

            ProposalResponse response = votingService.transitionProposalStatus(
                    22L, ProposalStatus.EXPIRED, "Deadline elapsed without quorum", 5L
            );

            assertEquals(ProposalStatus.EXPIRED, response.getStatus());
        }

        @Test
        @DisplayName("Should strictly reject transitioning from terminal PASSED status")
        void transition_rejectsFromPassed() {
            Proposal proposal = new Proposal();
            proposal.setId(23L);
            proposal.setGroup(sampleGroup);
            proposal.setStatus(ProposalStatus.PASSED);

            when(proposalRepository.findById(23L)).thenReturn(Optional.of(proposal));

            assertThrows(com.example.evshare.exception.InvalidProposalStateTransitionException.class, () ->
                    votingService.transitionProposalStatus(23L, ProposalStatus.ACTIVE, "Reopening passed vote", 5L));

            verify(proposalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should strictly reject transitioning from terminal REJECTED status")
        void transition_rejectsFromRejected() {
            Proposal proposal = new Proposal();
            proposal.setId(24L);
            proposal.setGroup(sampleGroup);
            proposal.setStatus(ProposalStatus.REJECTED);

            when(proposalRepository.findById(24L)).thenReturn(Optional.of(proposal));

            assertThrows(com.example.evshare.exception.InvalidProposalStateTransitionException.class, () ->
                    votingService.transitionProposalStatus(24L, ProposalStatus.PASSED, "Overriding rejection", 5L));
        }

        @Test
        @DisplayName("Should strictly reject transitioning from terminal EXPIRED status")
        void transition_rejectsFromExpired() {
            Proposal proposal = new Proposal();
            proposal.setId(25L);
            proposal.setGroup(sampleGroup);
            proposal.setStatus(ProposalStatus.EXPIRED);

            when(proposalRepository.findById(25L)).thenReturn(Optional.of(proposal));

            assertThrows(com.example.evshare.exception.InvalidProposalStateTransitionException.class, () ->
                    votingService.transitionProposalStatus(25L, ProposalStatus.REJECTED, "Re-expiring", 5L));
        }

        @Test
        @DisplayName("Should strictly reject self-transition (ACTIVE -> ACTIVE)")
        void transition_rejectsSelfTransition() {
            Proposal proposal = new Proposal();
            proposal.setId(26L);
            proposal.setGroup(sampleGroup);
            proposal.setStatus(ProposalStatus.ACTIVE);

            when(proposalRepository.findById(26L)).thenReturn(Optional.of(proposal));

            assertThrows(com.example.evshare.exception.InvalidProposalStateTransitionException.class, () ->
                    votingService.transitionProposalStatus(26L, ProposalStatus.ACTIVE, "No-op transition", 5L));
        }

        @Test
        @DisplayName("Should retrieve chronological proposal history from audit logs")
        void getProposalHistory_success() {
            AuditLog log1 = new AuditLog();
            log1.setId(101L);
            log1.setAction("PROPOSAL_CREATED");
            log1.setEntityId(30L);
            log1.setUser(sampleUser);
            log1.setNewStateJson("{\"status\":\"ACTIVE\"}");
            log1.setCreatedAt(Instant.now().minus(2, ChronoUnit.HOURS));

            AuditLog log2 = new AuditLog();
            log2.setId(102L);
            log2.setAction("PROPOSAL_STATE_TRANSITION");
            log2.setEntityId(30L);
            log2.setUser(sampleUser);
            log2.setOldStateJson("{\"status\":\"ACTIVE\"}");
            log2.setNewStateJson("{\"status\":\"PASSED\"}");
            log2.setCreatedAt(Instant.now());

            when(proposalRepository.existsById(30L)).thenReturn(true);
            when(auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Proposal", 30L))
                    .thenReturn(List.of(log2, log1));

            List<com.example.evshare.dto.response.ProposalAuditLogResponse> history = votingService.getProposalHistory(30L);

            assertNotNull(history);
            assertEquals(2, history.size());
            assertEquals("PROPOSAL_STATE_TRANSITION", history.get(0).getAction());
            assertEquals("PROPOSAL_CREATED", history.get(1).getAction());
        }

        @Test
        @DisplayName("Should auto-expire ACTIVE proposal when voting deadline is in the past")
        void checkAndExpire_expiresWhenOverdue() {
            Proposal overdue = new Proposal();
            overdue.setId(40L);
            overdue.setGroup(sampleGroup);
            overdue.setProposerUser(sampleUser);
            overdue.setStatus(ProposalStatus.ACTIVE);
            overdue.setVotingDeadline(Instant.now().minus(1, ChronoUnit.HOURS));

            when(proposalRepository.findById(40L)).thenReturn(Optional.of(overdue));
            when(proposalRepository.save(any(Proposal.class))).thenAnswer(inv -> inv.getArgument(0));
            when(voteOptionRepository.findByProposalId(40L)).thenReturn(List.of());

            ProposalResponse response = votingService.checkAndExpireIfDeadlinePassed(40L);

            assertEquals(ProposalStatus.EXPIRED, response.getStatus());
            assertEquals(ProposalStatus.EXPIRED, overdue.getStatus());
        }

        @Test
        @DisplayName("Should not expire ACTIVE proposal when deadline is still in the future")
        void checkAndExpire_remainsActiveWhenInFuture() {
            Proposal active = new Proposal();
            active.setId(41L);
            active.setGroup(sampleGroup);
            active.setProposerUser(sampleUser);
            active.setStatus(ProposalStatus.ACTIVE);
            active.setVotingDeadline(Instant.now().plus(24, ChronoUnit.HOURS));

            when(proposalRepository.findById(41L)).thenReturn(Optional.of(active));
            when(voteOptionRepository.findByProposalId(41L)).thenReturn(List.of());

            ProposalResponse response = votingService.checkAndExpireIfDeadlinePassed(41L);

            assertEquals(ProposalStatus.ACTIVE, response.getStatus());
            verify(proposalRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Checkpoint 07-D — Proposer Eligibility Tests (BR-VOT-01)")
    class ProposerEligibilityTests {

        @Test
        @DisplayName("validateProposerEligibility: 10% active equity is permitted")
        void validateProposerEligibility_exactly10Percent_permitted() {
            sampleShare.setPercentage(new BigDecimal("10.00"));
            sampleShare.setIsActive(true);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            assertDoesNotThrow(() -> votingService.validateProposerEligibility(1L, 5L));
        }

        @Test
        @DisplayName("validateProposerEligibility: >10% active equity is permitted")
        void validateProposerEligibility_greaterThan10Percent_permitted() {
            sampleShare.setPercentage(new BigDecimal("25.00"));
            sampleShare.setIsActive(true);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            assertDoesNotThrow(() -> votingService.validateProposerEligibility(1L, 5L));
        }

        @Test
        @DisplayName("validateProposerEligibility: <10% active equity is rejected with InsufficientEquityException")
        void validateProposerEligibility_lessThan10Percent_rejected() {
            sampleShare.setPercentage(new BigDecimal("9.99"));
            sampleShare.setIsActive(true);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            InsufficientEquityException ex = assertThrows(InsufficientEquityException.class,
                    () -> votingService.validateProposerEligibility(1L, 5L));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("at least 10.00% active equity"));
        }

        @Test
        @DisplayName("validateProposerEligibility: non-owner is rejected with BusinessException HTTP 403")
        void validateProposerEligibility_nonOwner_rejected() {
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.validateProposerEligibility(1L, 99L));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("not an active co-owner"));
        }

        @Test
        @DisplayName("validateProposerEligibility: inactive owner is rejected with BusinessException HTTP 403 even if percentage >= 10%")
        void validateProposerEligibility_inactiveOwner_rejected() {
            sampleShare.setPercentage(new BigDecimal("50.00"));
            sampleShare.setIsActive(false);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.validateProposerEligibility(1L, 5L));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("inactive co-owner"));
        }

        @Test
        @DisplayName("createProposal: rejects proposal when proposer is an inactive owner")
        void createProposal_inactiveOwner_rejected() {
            sampleShare.setPercentage(new BigDecimal("30.00"));
            sampleShare.setIsActive(false);

            CreateProposalRequest request = new CreateProposalRequest(
                    1L,
                    "Proposal by inactive member",
                    "Rationale",
                    ProposalType.ROUTINE_EXPENSE,
                    null
            );

            when(ownershipGroupRepository.findById(1L)).thenReturn(Optional.of(sampleGroup));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.createProposal(request, 5L));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("inactive co-owner"));
            verify(proposalRepository, never()).save(any());
        }

        @Test
        @DisplayName("checkProposerEligibility: returns eligible=true for active owner >= 10%")
        void checkProposerEligibility_eligible() {
            sampleShare.setPercentage(new BigDecimal("15.00"));
            sampleShare.setIsActive(true);

            when(ownershipGroupRepository.existsById(1L)).thenReturn(true);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            ProposerEligibilityResponse res = votingService.checkProposerEligibility(1L, 5L);

            assertNotNull(res);
            assertTrue(res.isEligible());
            assertTrue(res.isGroupMember());
            assertTrue(res.isActiveMember());
            assertEquals(new BigDecimal("15.00"), res.getEquityPercentage());
            assertEquals(new BigDecimal("10.00"), res.getRequiredPercentage());
        }

        @Test
        @DisplayName("checkProposerEligibility: returns eligible=false for active owner < 10%")
        void checkProposerEligibility_lessThan10Percent() {
            sampleShare.setPercentage(new BigDecimal("8.50"));
            sampleShare.setIsActive(true);

            when(ownershipGroupRepository.existsById(1L)).thenReturn(true);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            ProposerEligibilityResponse res = votingService.checkProposerEligibility(1L, 5L);

            assertNotNull(res);
            assertFalse(res.isEligible());
            assertTrue(res.isGroupMember());
            assertTrue(res.isActiveMember());
            assertEquals(new BigDecimal("8.50"), res.getEquityPercentage());
            assertTrue(res.getReason().contains("below the required 10.00%"));
        }

        @Test
        @DisplayName("checkProposerEligibility: returns eligible=false for inactive owner")
        void checkProposerEligibility_inactiveOwner() {
            sampleShare.setPercentage(new BigDecimal("40.00"));
            sampleShare.setIsActive(false);

            when(ownershipGroupRepository.existsById(1L)).thenReturn(true);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            ProposerEligibilityResponse res = votingService.checkProposerEligibility(1L, 5L);

            assertNotNull(res);
            assertFalse(res.isEligible());
            assertTrue(res.isGroupMember());
            assertFalse(res.isActiveMember());
            assertTrue(res.getReason().contains("inactive co-owner"));
        }

        @Test
        @DisplayName("checkProposerEligibility: returns eligible=false for non-owner")
        void checkProposerEligibility_nonOwner() {
            when(ownershipGroupRepository.existsById(1L)).thenReturn(true);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

            ProposerEligibilityResponse res = votingService.checkProposerEligibility(1L, 99L);

            assertNotNull(res);
            assertFalse(res.isEligible());
            assertFalse(res.isGroupMember());
            assertFalse(res.isActiveMember());
            assertTrue(res.getReason().contains("not an active co-owner"));
        }

        @Test
        @DisplayName("checkProposerEligibility: throws ResourceNotFoundException when group missing")
        void checkProposerEligibility_groupNotFound() {
            when(ownershipGroupRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () -> votingService.checkProposerEligibility(999L, 5L));
        }
    }

    @Nested
    @DisplayName("Checkpoint 07-E — Vote Casting Tests")
    class VoteCastingTests {

        private Proposal activeProposal;
        private VoteOption approveOpt;
        private VoteOption rejectOpt;
        private VoteOption abstainOpt;

        @BeforeEach
        void setupProposalAndOptions() {
            activeProposal = new Proposal();
            activeProposal.setId(50L);
            activeProposal.setGroup(sampleGroup);
            activeProposal.setProposerUser(sampleUser);
            activeProposal.setTitle("Air Conditioning Compressor Replacement");
            activeProposal.setDescription("Routine expense replacement");
            activeProposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
            activeProposal.setStatus(ProposalStatus.ACTIVE);
            activeProposal.setVotingDeadline(Instant.now().plus(72, ChronoUnit.HOURS));

            approveOpt = new VoteOption(1L, activeProposal, VoteOptionKey.APPROVE, "Approve");
            rejectOpt = new VoteOption(2L, activeProposal, VoteOptionKey.REJECT, "Reject");
            abstainOpt = new VoteOption(3L, activeProposal, VoteOptionKey.ABSTAIN, "Abstain");
        }

        @Test
        @DisplayName("castVote: casts APPROVE ballot successfully with authoritative equity weight")
        void castVote_approve_success() {
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));
            when(voteRepository.existsByProposalIdAndUserId(50L, 5L)).thenReturn(false);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(voteOptionRepository.findByProposalIdAndOptionKey(50L, VoteOptionKey.APPROVE)).thenReturn(Optional.of(approveOpt));
            when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(inv -> {
                Vote v = inv.getArgument(0);
                v.setId(901L);
                return v;
            });

            VoteResponse response = votingService.castVote(50L, request, 5L);

            assertNotNull(response);
            assertEquals(901L, response.getId());
            assertEquals(50L, response.getProposalId());
            assertEquals(5L, response.getUserId());
            assertEquals(VoteOptionKey.APPROVE, response.getOptionKey());
            assertEquals(new BigDecimal("25.00"), response.getEquityWeight());

            verify(voteRepository).saveAndFlush(any(Vote.class));
            verify(auditLogRepository).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("castVote: casts REJECT ballot successfully")
        void castVote_reject_success() {
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.REJECT);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));
            when(voteRepository.existsByProposalIdAndUserId(50L, 5L)).thenReturn(false);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(voteOptionRepository.findByProposalIdAndOptionKey(50L, VoteOptionKey.REJECT)).thenReturn(Optional.of(rejectOpt));
            when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(inv -> {
                Vote v = inv.getArgument(0);
                v.setId(902L);
                return v;
            });

            VoteResponse response = votingService.castVote(50L, request, 5L);

            assertNotNull(response);
            assertEquals(VoteOptionKey.REJECT, response.getOptionKey());
            assertEquals(new BigDecimal("25.00"), response.getEquityWeight());
        }

        @Test
        @DisplayName("castVote: casts ABSTAIN ballot successfully")
        void castVote_abstain_success() {
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.ABSTAIN);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));
            when(voteRepository.existsByProposalIdAndUserId(50L, 5L)).thenReturn(false);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(voteOptionRepository.findByProposalIdAndOptionKey(50L, VoteOptionKey.ABSTAIN)).thenReturn(Optional.of(abstainOpt));
            when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(inv -> {
                Vote v = inv.getArgument(0);
                v.setId(903L);
                return v;
            });

            VoteResponse response = votingService.castVote(50L, request, 5L);

            assertNotNull(response);
            assertEquals(VoteOptionKey.ABSTAIN, response.getOptionKey());
        }

        @Test
        @DisplayName("castVote: duplicate vote rejected with HTTP 409 Conflict")
        void castVote_duplicateVote_rejectedWithConflict() {
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));
            when(voteRepository.existsByProposalIdAndUserId(50L, 5L)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.castVote(50L, request, 5L));

            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("already cast a ballot"));
            verify(voteRepository, never()).saveAndFlush(any(Vote.class));
        }

        @Test
        @DisplayName("castVote: database unique constraint race translated to HTTP 409 Conflict")
        void castVote_duplicateVote_dbConstraintViolation_translatedToConflict() {
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));
            when(voteRepository.existsByProposalIdAndUserId(50L, 5L)).thenReturn(false);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(voteOptionRepository.findByProposalIdAndOptionKey(50L, VoteOptionKey.APPROVE)).thenReturn(Optional.of(approveOpt));
            when(voteRepository.saveAndFlush(any(Vote.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry '50-5' for key 'uk_proposal_user_vote'"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.castVote(50L, request, 5L));

            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertTrue(ex.getMessage().contains("already cast a ballot"));
        }

        @Test
        @DisplayName("castVote: same voter can vote on different proposals")
        void castVote_differentProposals_sameVoter_permitted() {
            Proposal secondProposal = new Proposal();
            secondProposal.setId(51L);
            secondProposal.setGroup(sampleGroup);
            secondProposal.setProposerUser(sampleUser);
            secondProposal.setStatus(ProposalStatus.ACTIVE);
            secondProposal.setVotingDeadline(Instant.now().plus(72, ChronoUnit.HOURS));

            VoteOption secondApproveOpt = new VoteOption(4L, secondProposal, VoteOptionKey.APPROVE, "Approve");

            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            when(proposalRepository.findById(51L)).thenReturn(Optional.of(secondProposal));
            when(voteRepository.existsByProposalIdAndUserId(51L, 5L)).thenReturn(false);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));
            when(userRepository.findById(5L)).thenReturn(Optional.of(sampleUser));
            when(voteOptionRepository.findByProposalIdAndOptionKey(51L, VoteOptionKey.APPROVE)).thenReturn(Optional.of(secondApproveOpt));
            when(voteRepository.saveAndFlush(any(Vote.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> votingService.castVote(51L, request, 5L));
        }

        @Test
        @DisplayName("castVote: non-ACTIVE proposal rejected with HTTP 400 Bad Request")
        void castVote_nonActiveProposal_rejectedWithBadRequest() {
            activeProposal.setStatus(ProposalStatus.PASSED);
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.castVote(50L, request, 5L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("ACTIVE"));
        }

        @Test
        @DisplayName("castVote: expired voting deadline rejected with HTTP 400 Bad Request")
        void castVote_expiredDeadline_rejectedWithBadRequest() {
            activeProposal.setVotingDeadline(Instant.now().minus(1, ChronoUnit.HOURS));
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.castVote(50L, request, 5L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("expired"));
        }

        @Test
        @DisplayName("castVote: non-owner rejected with HTTP 403 Forbidden")
        void castVote_nonOwner_rejectedWithForbidden() {
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));
            when(voteRepository.existsByProposalIdAndUserId(50L, 99L)).thenReturn(false);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.castVote(50L, request, 99L));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("not an active co-owner"));
        }

        @Test
        @DisplayName("castVote: inactive owner rejected with HTTP 403 Forbidden")
        void castVote_inactiveOwner_rejectedWithForbidden() {
            sampleShare.setIsActive(false);
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            when(proposalRepository.findById(50L)).thenReturn(Optional.of(activeProposal));
            when(voteRepository.existsByProposalIdAndUserId(50L, 5L)).thenReturn(false);
            when(ownershipShareRepository.findByGroupIdAndUserId(1L, 5L)).thenReturn(Optional.of(sampleShare));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.castVote(50L, request, 5L));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertTrue(ex.getMessage().contains("inactive co-owner"));
        }

        @Test
        @DisplayName("castVote: unauthenticated voter rejected with HTTP 401 Unauthorized")
        void castVote_unauthenticated_rejected() {
            CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> votingService.castVote(50L, request, null));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        }

        @Test
        @DisplayName("getVotesByProposalId: returns list of cast ballots")
        void getVotesByProposalId_success() {
            Vote vote1 = new Vote(1L, activeProposal, sampleUser, approveOpt, new BigDecimal("25.00"), Instant.now());
            when(proposalRepository.existsById(50L)).thenReturn(true);
            when(voteRepository.findByProposalId(50L)).thenReturn(List.of(vote1));

            List<VoteResponse> votes = votingService.getVotesByProposalId(50L);

            assertNotNull(votes);
            assertEquals(1, votes.size());
            assertEquals(VoteOptionKey.APPROVE, votes.get(0).getOptionKey());
        }

        @Test
        @DisplayName("getVoteByProposalAndUser: returns user ballot if present")
        void getVoteByProposalAndUser_success() {
            Vote vote1 = new Vote(1L, activeProposal, sampleUser, approveOpt, new BigDecimal("25.00"), Instant.now());
            when(voteRepository.findByProposalIdAndUserId(50L, 5L)).thenReturn(Optional.of(vote1));

            Optional<VoteResponse> voteOpt = votingService.getVoteByProposalAndUser(50L, 5L);

            assertTrue(voteOpt.isPresent());
            assertEquals(VoteOptionKey.APPROVE, voteOpt.get().getOptionKey());
        }
    }

    @Nested
    @DisplayName("07-G: Equity Weighted Voting Tests")
    class EquityWeightedVotingTests {

        private User userA;
        private User userB;
        private User userC;

        private VoteOption optApprove;
        private VoteOption optReject;
        private VoteOption optAbstain;

        private User createMockUser(Long id, String name, String email) {
            User u = new User();
            u.setId(id);
            u.setFullName(name);
            u.setEmail(email);
            u.setIsActive(true);
            return u;
        }

        @BeforeEach
        void setUpEquityScenario() {
            userA = createMockUser(101L, "Alice Owner", "alice@syndicate.io");
            userB = createMockUser(102L, "Bob Owner", "bob@syndicate.io");
            userC = createMockUser(103L, "Charlie Owner", "charlie@syndicate.io");

            optApprove = new VoteOption(201L, null, VoteOptionKey.APPROVE, "Approve proposal");
            optReject = new VoteOption(202L, null, VoteOptionKey.REJECT, "Reject proposal");
            optAbstain = new VoteOption(203L, null, VoteOptionKey.ABSTAIN, "Abstain from voting");
        }

        @Test
        @DisplayName("07-G: 40/30/30 ownership - 40% APPROVE beats 30% REJECT despite 1-1 headcount tie")
        void testTally_40_30_30_ownership_routineExpense() {
            // Group with 100% active equity (Alice: 40%, Bob: 30%, Charlie: 30%)
            Proposal proposal = new Proposal();
            proposal.setId(701L);
            proposal.setTitle("Replace AC Compressor");
            proposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
            proposal.setStatus(ProposalStatus.ACTIVE);
            proposal.setGroup(sampleGroup);

            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("40.00"), Instant.now());
            Vote voteB = new Vote(2L, proposal, userB, optReject, new BigDecimal("30.00"), Instant.now());
            // Charlie (30%) abstains from casting a ballot

            when(proposalRepository.findById(701L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(701L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(701L);

            assertNotNull(tally);
            assertEquals(701L, tally.getProposalId());
            assertEquals(new BigDecimal("100.00"), tally.getTotalGroupActiveEquity());
            assertEquals(new BigDecimal("70.00"), tally.getTotalParticipatingEquity()); // 40 + 30 = 70%
            assertEquals(2, tally.getTotalVotersCount());
            assertTrue(tally.isQuorumReached(), "Quorum of 60% reached (70.00% participating)");
            assertEquals(new BigDecimal("40.00"), tally.getApproveEquity());
            assertEquals(new BigDecimal("30.00"), tally.getRejectEquity());
            assertEquals(new BigDecimal("0.00"), tally.getAbstainEquity());

            // 40.00 / 70.00 = 57.14% of participating equity (>50.00% required for ROUTINE_EXPENSE)
            assertEquals(new BigDecimal("57.14"), tally.getApprovePercentageOfParticipating());
            assertEquals(new BigDecimal("42.86"), tally.getRejectPercentageOfParticipating());
            assertTrue(tally.isPassed(), "Equity-weighted voting: 40% beats 30% despite 1-1 headcount tie");
            assertTrue(tally.getOutcomeReason().contains("Approved with 57.14%"));
            assertEquals(2, tally.getBallots().size());
        }

        @Test
        @DisplayName("07-G: Uneven ownership (55.50 / 24.25 / 20.25) - 55.50% APPROVE beats two REJECTs (1 vs 2 headcount)")
        void testTally_uneven_ownership_routineExpense() {
            // Group: Alice 55.50%, Bob 24.25%, Charlie 20.25%
            Proposal proposal = new Proposal();
            proposal.setId(702L);
            proposal.setTitle("Upgrade Firmware at Service Center");
            proposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
            proposal.setStatus(ProposalStatus.ACTIVE);
            proposal.setGroup(sampleGroup);

            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("55.50"), Instant.now());
            Vote voteB = new Vote(2L, proposal, userB, optReject, new BigDecimal("24.25"), Instant.now());
            Vote voteC = new Vote(3L, proposal, userC, optReject, new BigDecimal("20.25"), Instant.now());

            when(proposalRepository.findById(702L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(702L)).thenReturn(List.of(voteA, voteB, voteC));

            ProposalTallyResponse tally = votingService.getProposalTally(702L);

            assertNotNull(tally);
            assertEquals(new BigDecimal("100.00"), tally.getTotalParticipatingEquity());
            assertEquals(3, tally.getTotalVotersCount());
            assertTrue(tally.isQuorumReached());
            assertEquals(new BigDecimal("55.50"), tally.getApproveEquity());
            assertEquals(new BigDecimal("44.50"), tally.getRejectEquity());
            assertEquals(new BigDecimal("55.50"), tally.getApprovePercentageOfParticipating());
            // In one-user-one-vote, 1 vs 2 would fail (33% vs 67%). In equity-weighted voting, 55.50% passes!
            assertTrue(tally.isPassed(), "Equity weight ensures majority shareholder prevails over two minority shareholders");
            assertEquals(3, tally.getBallots().size());
        }

        @Test
        @DisplayName("07-G: Uneven ownership supermajority (MAJOR_EXPENSE) fails when APPROVE < 75.00%")
        void testTally_uneven_ownership_supermajority_fails() {
            // Major expense requires >= 75.00% of total group active equity
            Proposal proposal = new Proposal();
            proposal.setId(703L);
            proposal.setTitle("Complete Battery Pack Replacement ($8,000)");
            proposal.setProposalType(ProposalType.MAJOR_EXPENSE);
            proposal.setStatus(ProposalStatus.ACTIVE);
            proposal.setGroup(sampleGroup);

            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("55.50"), Instant.now());
            Vote voteB = new Vote(2L, proposal, userB, optReject, new BigDecimal("24.25"), Instant.now());
            Vote voteC = new Vote(3L, proposal, userC, optAbstain, new BigDecimal("20.25"), Instant.now());

            when(proposalRepository.findById(703L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(703L)).thenReturn(List.of(voteA, voteB, voteC));

            ProposalTallyResponse tally = votingService.getProposalTally(703L);

            assertNotNull(tally);
            assertTrue(tally.isQuorumReached()); // 100% participation
            assertEquals(new BigDecimal("55.50"), tally.getApproveEquity());
            assertFalse(tally.isPassed(), "55.50% < 75.00% supermajority threshold required for MAJOR_EXPENSE");
            assertTrue(tally.getOutcomeReason().contains("Failed 75.00% supermajority threshold"));
        }

        @Test
        @DisplayName("07-G: Inactive share is excluded from group active equity and cannot participate")
        void testTally_inactiveShare_excludedFromVotingAndActiveTotal() {
            // Alice (40% active), Bob (30% active), Charlie (30% inactive)
            // Active syndicate equity is only 70.00%
            Proposal proposal = new Proposal();
            proposal.setId(704L);
            proposal.setTitle("Emergency Brake Pad Replacement");
            proposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
            proposal.setStatus(ProposalStatus.ACTIVE);
            proposal.setGroup(sampleGroup);

            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("40.00"), Instant.now());
            Vote voteB = new Vote(2L, proposal, userB, optApprove, new BigDecimal("30.00"), Instant.now());
            // Charlie is inactive and cannot cast a vote

            when(proposalRepository.findById(704L)).thenReturn(Optional.of(proposal));
            // Database query returns sum of isActive = true shares: 40 + 30 = 70.00%
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("70.00"));
            when(voteRepository.findByProposalId(704L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(704L);

            assertNotNull(tally);
            assertEquals(new BigDecimal("70.00"), tally.getTotalGroupActiveEquity(), "Only active equity is counted");
            assertEquals(new BigDecimal("70.00"), tally.getTotalParticipatingEquity());
            assertTrue(tally.isQuorumReached()); // 70.00 >= 60.00
            assertEquals(new BigDecimal("70.00"), tally.getApproveEquity());
            assertEquals(new BigDecimal("100.00"), tally.getApprovePercentageOfParticipating());
            assertEquals(new BigDecimal("100.00"), tally.getApprovePercentageOfTotal(), "70/70 = 100% of active equity");
            assertTrue(tally.isPassed());
        }

        @Test
        @DisplayName("07-G: Quorum not reached fails proposal regardless of 100% approval rate")
        void testTally_quorumNotReached_fails() {
            Proposal proposal = new Proposal();
            proposal.setId(705L);
            proposal.setTitle("Install Custom Seat Covers");
            proposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
            proposal.setStatus(ProposalStatus.ACTIVE);
            proposal.setGroup(sampleGroup);

            // Alice (40%) votes APPROVE, Bob (30%) and Charlie (30%) do not vote. Participating: 40% < 60%
            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("40.00"), Instant.now());

            when(proposalRepository.findById(705L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(705L)).thenReturn(List.of(voteA));

            ProposalTallyResponse tally = votingService.getProposalTally(705L);

            assertNotNull(tally);
            assertEquals(new BigDecimal("40.00"), tally.getTotalParticipatingEquity());
            assertFalse(tally.isQuorumReached(), "Quorum of 60.00% is NOT reached with only 40.00% participation");
            assertFalse(tally.isPassed(), "Proposal must fail if quorum is not reached");
            assertTrue(tally.getOutcomeReason().contains("Quorum of 60.00% not reached"));
        }

        @Test
        @DisplayName("07-G: Preserves complete ballot audit history")
        void testTally_preservesVoteHistory() {
            Proposal proposal = new Proposal();
            proposal.setId(706L);
            proposal.setTitle("Syndicate Rule Revision");
            proposal.setProposalType(ProposalType.OPERATIONAL_RULE_CHANGE);
            proposal.setStatus(ProposalStatus.ACTIVE);
            proposal.setGroup(sampleGroup);

            Instant t1 = Instant.now().minusSeconds(100);
            Instant t2 = Instant.now().minusSeconds(50);
            Vote voteA = new Vote(10L, proposal, userA, optApprove, new BigDecimal("40.00"), t1);
            Vote voteB = new Vote(11L, proposal, userB, optAbstain, new BigDecimal("30.00"), t2);

            when(proposalRepository.findById(706L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(706L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(706L);

            assertNotNull(tally);
            assertEquals(2, tally.getBallots().size());

            VoteResponse b1 = tally.getBallots().get(0);
            assertEquals(10L, b1.getId());
            assertEquals(101L, b1.getUserId());
            assertEquals("Alice Owner", b1.getVoterName());
            assertEquals(VoteOptionKey.APPROVE, b1.getOptionKey());
            assertEquals(new BigDecimal("40.00"), b1.getEquityWeight());
            assertEquals(t1, b1.getVotedAt());

            VoteResponse b2 = tally.getBallots().get(1);
            assertEquals(11L, b2.getId());
            assertEquals(102L, b2.getUserId());
            assertEquals("Bob Owner", b2.getVoterName());
            assertEquals(VoteOptionKey.ABSTAIN, b2.getOptionKey());
            assertEquals(new BigDecimal("30.00"), b2.getEquityWeight());
            assertEquals(t2, b2.getVotedAt());
        }
    }

    @Nested
    @DisplayName("07-H: Quorum Evaluation Tests (>= 60% Active Equity Participation)")
    class QuorumTests {

        private User userA;
        private User userB;
        private VoteOption optApprove;
        private VoteOption optReject;
        private VoteOption optAbstain;

        private User createMockUser(Long id, String name, String email) {
            User u = new User();
            u.setId(id);
            u.setFullName(name);
            u.setEmail(email);
            u.setIsActive(true);
            return u;
        }

        @BeforeEach
        void setUp() {
            userA = createMockUser(201L, "Voter A", "a@syndicate.io");
            userB = createMockUser(202L, "Voter B", "b@syndicate.io");
            optApprove = new VoteOption(301L, null, VoteOptionKey.APPROVE, "Approve");
            optReject = new VoteOption(302L, null, VoteOptionKey.REJECT, "Reject");
            optAbstain = new VoteOption(303L, null, VoteOptionKey.ABSTAIN, "Abstain");
        }

        private Proposal createProposal(Long id, ProposalType type) {
            Proposal p = new Proposal();
            p.setId(id);
            p.setTitle("Quorum Test Proposal " + id);
            p.setProposalType(type);
            p.setStatus(ProposalStatus.ACTIVE);
            p.setGroup(sampleGroup);
            return p;
        }

        @Test
        @DisplayName("07-H Quorum: 59.99% participation strictly fails 60.00% quorum threshold")
        void testQuorum_59_99_percent_failsQuorum() {
            Proposal proposal = createProposal(801L, ProposalType.ROUTINE_EXPENSE);
            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("59.99"), Instant.now());

            when(proposalRepository.findById(801L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(801L)).thenReturn(List.of(voteA));

            ProposalTallyResponse tally = votingService.getProposalTally(801L);

            assertNotNull(tally);
            assertEquals(new BigDecimal("100.00"), tally.getTotalGroupActiveEquity());
            assertEquals(new BigDecimal("59.99"), tally.getTotalParticipatingEquity());
            assertEquals(new BigDecimal("59.99"), tally.getParticipationRatePercentage());
            assertFalse(tally.isQuorumReached(), "59.99% participation must NOT meet the 60.00% quorum");
            assertFalse(tally.isPassed(), "Proposal cannot pass when quorum fails");
            assertTrue(tally.getOutcomeReason().contains("Quorum of 60.00% not reached"));
        }

        @Test
        @DisplayName("07-H Quorum: Exactly 60.00% participation meets quorum threshold (boundary inclusion)")
        void testQuorum_exactly_60_00_percent_meetsQuorum() {
            Proposal proposal = createProposal(802L, ProposalType.ROUTINE_EXPENSE);
            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("60.00"), Instant.now());

            when(proposalRepository.findById(802L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(802L)).thenReturn(List.of(voteA));

            ProposalTallyResponse tally = votingService.getProposalTally(802L);

            assertNotNull(tally);
            assertEquals(new BigDecimal("100.00"), tally.getTotalGroupActiveEquity());
            assertEquals(new BigDecimal("60.00"), tally.getTotalParticipatingEquity());
            assertEquals(new BigDecimal("60.00"), tally.getParticipationRatePercentage());
            assertTrue(tally.isQuorumReached(), "Exactly 60.00% participation satisfies >= 60.00% quorum");
            assertTrue(tally.isPassed());
            assertTrue(tally.getOutcomeReason().contains("Approved"));
        }

        @Test
        @DisplayName("07-H Quorum: >60% participation (e.g. 60.01%) satisfies quorum threshold")
        void testQuorum_greaterThan_60_percent_meetsQuorum() {
            Proposal proposal = createProposal(803L, ProposalType.ROUTINE_EXPENSE);
            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("40.00"), Instant.now());
            Vote voteB = new Vote(2L, proposal, userB, optReject, new BigDecimal("20.01"), Instant.now());

            when(proposalRepository.findById(803L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(803L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(803L);

            assertNotNull(tally);
            assertEquals(new BigDecimal("60.01"), tally.getTotalParticipatingEquity());
            assertEquals(new BigDecimal("60.01"), tally.getParticipationRatePercentage());
            assertTrue(tally.isQuorumReached(), "60.01% satisfies >= 60.00% quorum");
            // 40.00 / 60.01 = 66.66% > 50%
            assertTrue(tally.isPassed());
        }

        @Test
        @DisplayName("07-H Quorum: ABSTAIN votes count toward reaching quorum but not toward approval")
        void testQuorum_abstainParticipation_countsTowardQuorum() {
            Proposal proposal = createProposal(804L, ProposalType.ROUTINE_EXPENSE);
            // User A (30%) votes APPROVE, User B (30%) votes ABSTAIN
            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("30.00"), Instant.now());
            Vote voteB = new Vote(2L, proposal, userB, optAbstain, new BigDecimal("30.00"), Instant.now());

            when(proposalRepository.findById(804L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(804L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(804L);

            assertNotNull(tally);
            assertEquals(new BigDecimal("60.00"), tally.getTotalParticipatingEquity(), "30% APPROVE + 30% ABSTAIN = 60% participating");
            assertEquals(new BigDecimal("60.00"), tally.getParticipationRatePercentage());
            assertTrue(tally.isQuorumReached(), "ABSTAIN ballots count directly toward quorum participation");
            assertEquals(new BigDecimal("30.00"), tally.getApproveEquity());
            assertEquals(new BigDecimal("30.00"), tally.getAbstainEquity());
            assertEquals(new BigDecimal("50.00"), tally.getApprovePercentageOfParticipating());

            // For ROUTINE_EXPENSE, approval requires STRICTLY > 50.00% of participating equity.
            // 30 / 60 = 50.00% which is NOT > 50.00%, so proposal does NOT pass.
            assertFalse(tally.isPassed(), "50.00% approval fails > 50.00% requirement even though quorum was met");
            assertTrue(tally.getOutcomeReason().contains("Did not achieve >50.00% approval"));
        }

        @Test
        @DisplayName("07-H Quorum: Zero votes cast results in 0.00% participation and fails quorum")
        void testQuorum_noVotes_failsQuorum() {
            Proposal proposal = createProposal(805L, ProposalType.ROUTINE_EXPENSE);

            when(proposalRepository.findById(805L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(805L)).thenReturn(List.of());

            ProposalTallyResponse tally = votingService.getProposalTally(805L);

            assertNotNull(tally);
            assertEquals(0, tally.getTotalVotersCount());
            assertEquals(new BigDecimal("0.00"), tally.getTotalParticipatingEquity());
            assertEquals(new BigDecimal("0.00"), tally.getParticipationRatePercentage());
            assertFalse(tally.isQuorumReached(), "0.00% participation fails quorum");
            assertFalse(tally.isPassed());
            assertTrue(tally.getOutcomeReason().contains("Quorum of 60.00% not reached"));
            assertTrue(tally.getBallots().isEmpty());
        }

        @Test
        @DisplayName("07-H Quorum: Participation calculated relative to active ownership equity when inactive shares exist")
        void testQuorum_calculatedUsingActiveOwnershipEquity_withInactiveShare() {
            Proposal proposal = createProposal(806L, ProposalType.ROUTINE_EXPENSE);
            // Group has only 75.00% active equity (25.00% inactive co-owner)
            // 60% quorum of 75.00% active equity = 45.00%
            Vote voteA = new Vote(1L, proposal, userA, optApprove, new BigDecimal("45.00"), Instant.now());

            when(proposalRepository.findById(806L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("75.00"));
            when(voteRepository.findByProposalId(806L)).thenReturn(List.of(voteA));

            ProposalTallyResponse tally = votingService.getProposalTally(806L);

            assertNotNull(tally);
            assertEquals(new BigDecimal("75.00"), tally.getTotalGroupActiveEquity());
            assertEquals(new BigDecimal("45.00"), tally.getTotalParticipatingEquity());
            // 45.00 / 75.00 = 60.00% of active equity
            assertEquals(new BigDecimal("60.00"), tally.getParticipationRatePercentage());
            assertTrue(tally.isQuorumReached(), "45.00 / 75.00 = 60.00% active participation satisfies quorum");
            assertTrue(tally.isPassed());
        }
    }

    @Nested
    @DisplayName("07-I: Voting Decision Threshold Tests (ROUTINE: >50%, MAJOR: >=75%, Quorum First)")
    class DecisionThresholdTests {

        private User voter1;
        private User voter2;
        private VoteOption optApprove;
        private VoteOption optReject;

        private User createMockUser(Long id, String name, String email) {
            User u = new User();
            u.setId(id);
            u.setFullName(name);
            u.setEmail(email);
            u.setIsActive(true);
            return u;
        }

        @BeforeEach
        void setUp() {
            voter1 = createMockUser(401L, "Voter 1", "v1@syndicate.io");
            voter2 = createMockUser(402L, "Voter 2", "v2@syndicate.io");
            optApprove = new VoteOption(501L, null, VoteOptionKey.APPROVE, "Approve");
            optReject = new VoteOption(502L, null, VoteOptionKey.REJECT, "Reject");
        }

        private Proposal createProposal(Long id, ProposalType type) {
            Proposal p = new Proposal();
            p.setId(id);
            p.setTitle("Threshold Test Proposal " + id);
            p.setProposalType(type);
            p.setStatus(ProposalStatus.ACTIVE);
            p.setGroup(sampleGroup);
            return p;
        }

        // ==========================================
        // ROUTINE THRESHOLD BOUNDARIES: > 50.00%
        // ==========================================

        @Test
        @DisplayName("07-I ROUTINE Boundary: Exactly 50.00% approval FAILS (strictly > 50% required)")
        void testThreshold_routine_exactly_50_00_percent_fails() {
            Proposal proposal = createProposal(901L, ProposalType.ROUTINE_EXPENSE);
            // 50.00% APPROVE vs 50.00% REJECT
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("50.00"), Instant.now());
            Vote voteB = new Vote(2L, proposal, voter2, optReject, new BigDecimal("50.00"), Instant.now());

            when(proposalRepository.findById(901L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(901L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(901L);

            assertNotNull(tally);
            assertTrue(tally.isQuorumReached(), "100% participation meets quorum");
            assertEquals(new BigDecimal("50.00"), tally.getRequiredThresholdPercentage());
            assertEquals("RELATIVE_TO_PARTICIPATING", tally.getThresholdType());
            assertEquals(new BigDecimal("50.00"), tally.getApprovePercentageOfParticipating());
            assertFalse(tally.isPassed(), "Exactly 50.00% must fail strictly > 50.00% threshold");
            assertTrue(tally.getOutcomeReason().contains("Did not achieve >50.00% approval"));
        }

        @Test
        @DisplayName("07-I ROUTINE Boundary: 50.01% approval PASSES (strictly > 50% satisfied)")
        void testThreshold_routine_50_01_percent_passes() {
            Proposal proposal = createProposal(902L, ProposalType.ROUTINE_EXPENSE);
            // 50.01% APPROVE vs 49.99% REJECT
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("50.01"), Instant.now());
            Vote voteB = new Vote(2L, proposal, voter2, optReject, new BigDecimal("49.99"), Instant.now());

            when(proposalRepository.findById(902L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(902L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(902L);

            assertNotNull(tally);
            assertTrue(tally.isQuorumReached());
            assertEquals(new BigDecimal("50.01"), tally.getApprovePercentageOfParticipating());
            assertTrue(tally.isPassed(), "50.01% exceeds 50.00% threshold");
            assertTrue(tally.getOutcomeReason().contains("Approved with 50.01%"));
        }

        @Test
        @DisplayName("07-I ROUTINE Boundary: 49.99% approval FAILS (> 50% not satisfied)")
        void testThreshold_routine_49_99_percent_fails() {
            Proposal proposal = createProposal(903L, ProposalType.ROUTINE_EXPENSE);
            // 49.99% APPROVE vs 50.01% REJECT
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("49.99"), Instant.now());
            Vote voteB = new Vote(2L, proposal, voter2, optReject, new BigDecimal("50.01"), Instant.now());

            when(proposalRepository.findById(903L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(903L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(903L);

            assertNotNull(tally);
            assertTrue(tally.isQuorumReached());
            assertEquals(new BigDecimal("49.99"), tally.getApprovePercentageOfParticipating());
            assertFalse(tally.isPassed());
            assertTrue(tally.getOutcomeReason().contains("Did not achieve >50.00% approval"));
        }

        // ==========================================
        // MAJOR THRESHOLD BOUNDARIES: >= 75.00%
        // ==========================================

        @Test
        @DisplayName("07-I MAJOR Boundary: 74.99% approval FAILS (>= 75.00% required)")
        void testThreshold_major_74_99_percent_fails() {
            Proposal proposal = createProposal(904L, ProposalType.MAJOR_EXPENSE);
            // 74.99% APPROVE vs 25.01% REJECT
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("74.99"), Instant.now());
            Vote voteB = new Vote(2L, proposal, voter2, optReject, new BigDecimal("25.01"), Instant.now());

            when(proposalRepository.findById(904L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(904L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(904L);

            assertNotNull(tally);
            assertTrue(tally.isQuorumReached());
            assertEquals(new BigDecimal("75.00"), tally.getRequiredThresholdPercentage());
            assertEquals("RELATIVE_TO_TOTAL", tally.getThresholdType());
            assertEquals(new BigDecimal("74.99"), tally.getApproveEquity());
            assertFalse(tally.isPassed(), "74.99% fails >= 75.00% threshold");
            assertTrue(tally.getOutcomeReason().contains("Failed 75.00% supermajority threshold"));
        }

        @Test
        @DisplayName("07-I MAJOR Boundary: Exactly 75.00% approval PASSES (>= 75.00% inclusive satisfaction)")
        void testThreshold_major_exactly_75_00_percent_passes() {
            Proposal proposal = createProposal(905L, ProposalType.MAJOR_EXPENSE);
            // 75.00% APPROVE vs 25.00% REJECT
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("75.00"), Instant.now());
            Vote voteB = new Vote(2L, proposal, voter2, optReject, new BigDecimal("25.00"), Instant.now());

            when(proposalRepository.findById(905L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(905L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(905L);

            assertNotNull(tally);
            assertTrue(tally.isQuorumReached());
            assertEquals(new BigDecimal("75.00"), tally.getApproveEquity());
            assertTrue(tally.isPassed(), "Exactly 75.00% satisfies >= 75.00% supermajority threshold");
            assertTrue(tally.getOutcomeReason().contains("Supermajority satisfied with 75.00%"));
        }

        @Test
        @DisplayName("07-I MAJOR Boundary: 75.01% approval PASSES (>= 75.00% satisfied)")
        void testThreshold_major_75_01_percent_passes() {
            Proposal proposal = createProposal(906L, ProposalType.OPERATIONAL_RULE_CHANGE);
            // 75.01% APPROVE vs 24.99% REJECT
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("75.01"), Instant.now());
            Vote voteB = new Vote(2L, proposal, voter2, optReject, new BigDecimal("24.99"), Instant.now());

            when(proposalRepository.findById(906L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(906L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(906L);

            assertNotNull(tally);
            assertTrue(tally.isQuorumReached());
            assertEquals(new BigDecimal("75.01"), tally.getApproveEquity());
            assertTrue(tally.isPassed());
        }

        @Test
        @DisplayName("07-I MAJOR: OWNER_ADMISSION_OR_EXIT applies >= 75.00% supermajority rule")
        void testThreshold_ownerAdmissionOrExit_appliesSupermajority() {
            Proposal proposal = createProposal(907L, ProposalType.OWNER_ADMISSION_OR_EXIT);
            // 70.00% APPROVE vs 10.00% REJECT
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("70.00"), Instant.now());
            Vote voteB = new Vote(2L, proposal, voter2, optReject, new BigDecimal("10.00"), Instant.now());

            when(proposalRepository.findById(907L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(907L)).thenReturn(List.of(voteA, voteB));

            ProposalTallyResponse tally = votingService.getProposalTally(907L);

            assertNotNull(tally);
            assertTrue(tally.isQuorumReached());
            assertEquals("RELATIVE_TO_TOTAL", tally.getThresholdType());
            assertEquals(new BigDecimal("70.00"), tally.getApproveEquity());
            // 70.00% < 75.00% -> fails
            assertFalse(tally.isPassed());
        }

        // ==========================================
        // APPLY QUORUM FIRST
        // ==========================================

        @Test
        @DisplayName("07-I Quorum First: 100.00% approval FAILS when quorum (<60%) is not achieved")
        void testThreshold_applyQuorumFirst_routineFailsWhenQuorumNotAchieved() {
            Proposal proposal = createProposal(908L, ProposalType.ROUTINE_EXPENSE);
            // Voter 1 has 40.00% and votes APPROVE. No other co-owner votes.
            // Participation is 40.00% < 60.00%. Approval among participating is 100.00%!
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("40.00"), Instant.now());

            when(proposalRepository.findById(908L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(908L)).thenReturn(List.of(voteA));

            ProposalTallyResponse tally = votingService.getProposalTally(908L);

            assertNotNull(tally);
            assertFalse(tally.isQuorumReached(), "Quorum of 60.00% must be applied first");
            assertEquals(new BigDecimal("100.00"), tally.getApprovePercentageOfParticipating());
            assertFalse(tally.isPassed(), "Proposal must fail when quorum is not met, even with 100% approval");
            assertTrue(tally.getOutcomeReason().contains("Quorum of 60.00% not reached"));
        }

        @Test
        @DisplayName("07-I Quorum First: Major proposal fails when quorum is not reached")
        void testThreshold_applyQuorumFirst_majorFailsWhenQuorumNotAchieved() {
            Proposal proposal = createProposal(909L, ProposalType.MAJOR_EXPENSE);
            // Voter 1 has 55.00% and votes APPROVE. Participation = 55.00% < 60.00%.
            Vote voteA = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("55.00"), Instant.now());

            when(proposalRepository.findById(909L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(909L)).thenReturn(List.of(voteA));

            ProposalTallyResponse tally = votingService.getProposalTally(909L);

            assertNotNull(tally);
            assertFalse(tally.isQuorumReached());
            assertFalse(tally.isPassed());
            assertTrue(tally.getOutcomeReason().contains("Quorum of 60.00% not reached"));
        }
    }

    @Nested
    @DisplayName("07-J: Proposal Voting Results Tests (GET /api/v1/proposals/{id}/results)")
    class ProposalResultsTests {

        private User voter1;
        private User voter2;
        private User voter3;
        private VoteOption optApprove;
        private VoteOption optReject;
        private VoteOption optAbstain;

        private User createMockUser(Long id, String name, String email) {
            User u = new User();
            u.setId(id);
            u.setFullName(name);
            u.setEmail(email);
            u.setIsActive(true);
            return u;
        }

        @BeforeEach
        void setUp() {
            voter1 = createMockUser(601L, "Voter 1", "v1@syndicate.io");
            voter2 = createMockUser(602L, "Voter 2", "v2@syndicate.io");
            voter3 = createMockUser(603L, "Voter 3", "v3@syndicate.io");
            optApprove = new VoteOption(701L, null, VoteOptionKey.APPROVE, "Approve");
            optReject = new VoteOption(702L, null, VoteOptionKey.REJECT, "Reject");
            optAbstain = new VoteOption(703L, null, VoteOptionKey.ABSTAIN, "Abstain");
        }

        private Proposal createProposal(Long id, ProposalType type, ProposalStatus status) {
            Proposal p = new Proposal();
            p.setId(id);
            p.setTitle("Results Proposal " + id);
            p.setProposalType(type);
            p.setStatus(status);
            p.setGroup(sampleGroup);
            return p;
        }

        @Test
        @DisplayName("07-J Results: Returns all required fields for passed routine proposal")
        void testGetProposalResults_routinePassed_returnsAllRequiredFields() {
            Proposal proposal = createProposal(1001L, ProposalType.ROUTINE_EXPENSE, ProposalStatus.ACTIVE);
            // 40% APPROVE, 20% REJECT, 10% ABSTAIN -> 70% participating (meets 60% quorum)
            Vote vote1 = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("40.00"), Instant.now());
            Vote vote2 = new Vote(2L, proposal, voter2, optReject, new BigDecimal("20.00"), Instant.now());
            Vote vote3 = new Vote(3L, proposal, voter3, optAbstain, new BigDecimal("10.00"), Instant.now());

            when(proposalRepository.findById(1001L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(1001L)).thenReturn(List.of(vote1, vote2, vote3));

            ProposalResultsResponse results = votingService.getProposalResults(1001L);

            assertNotNull(results);
            assertEquals(1001L, results.getProposalId());
            assertEquals("Results Proposal 1001", results.getProposalTitle());
            assertEquals(ProposalType.ROUTINE_EXPENSE, results.getProposalType());
            assertEquals(ProposalStatus.ACTIVE, results.getStatus());

            // 1. Total eligible equity
            assertEquals(new BigDecimal("100.00"), results.getTotalEligibleEquity());
            // 2. Participating equity
            assertEquals(new BigDecimal("70.00"), results.getParticipatingEquity());
            // 3. Approve weight
            assertEquals(new BigDecimal("40.00"), results.getApproveWeight());
            // 4. Reject weight
            assertEquals(new BigDecimal("20.00"), results.getRejectWeight());
            // 5. Abstain weight
            assertEquals(new BigDecimal("10.00"), results.getAbstainWeight());
            // 6. Quorum status
            assertEquals("REACHED", results.getQuorumStatus());
            assertTrue(results.isQuorumReached());
            // 7. Threshold
            assertEquals(new BigDecimal("50.00"), results.getThreshold());
            assertEquals("> 50.00% of participating equity", results.getThresholdDescription());
            // 8. Final decision
            assertEquals("PASSED", results.getFinalDecision());
            assertTrue(results.isPassed());
            assertNotNull(results.getDecisionReason());
        }

        @Test
        @DisplayName("07-J Results: Final decision is QUORUM_NOT_MET when participating equity < 60%")
        void testGetProposalResults_quorumNotMet() {
            Proposal proposal = createProposal(1002L, ProposalType.ROUTINE_EXPENSE, ProposalStatus.ACTIVE);
            // 40% APPROVE, no other votes -> 40% participating (<60% quorum)
            Vote vote1 = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("40.00"), Instant.now());

            when(proposalRepository.findById(1002L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(1002L)).thenReturn(List.of(vote1));

            ProposalResultsResponse results = votingService.getProposalResults(1002L);

            assertNotNull(results);
            assertEquals(new BigDecimal("100.00"), results.getTotalEligibleEquity());
            assertEquals(new BigDecimal("40.00"), results.getParticipatingEquity());
            assertEquals("NOT_REACHED", results.getQuorumStatus());
            assertFalse(results.isQuorumReached());
            assertEquals("QUORUM_NOT_MET", results.getFinalDecision());
            assertFalse(results.isPassed());
        }

        @Test
        @DisplayName("07-J Results: Final decision is REJECTED when quorum met but approval threshold not achieved")
        void testGetProposalResults_quorumMet_thresholdFailed_rejected() {
            Proposal proposal = createProposal(1003L, ProposalType.ROUTINE_EXPENSE, ProposalStatus.ACTIVE);
            // 30% APPROVE, 40% REJECT -> 70% participating (quorum met), approval is 30/70 = 42.86% < 50%
            Vote vote1 = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("30.00"), Instant.now());
            Vote vote2 = new Vote(2L, proposal, voter2, optReject, new BigDecimal("40.00"), Instant.now());

            when(proposalRepository.findById(1003L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(1003L)).thenReturn(List.of(vote1, vote2));

            ProposalResultsResponse results = votingService.getProposalResults(1003L);

            assertNotNull(results);
            assertEquals("REACHED", results.getQuorumStatus());
            assertTrue(results.isQuorumReached());
            assertEquals("REJECTED", results.getFinalDecision());
            assertFalse(results.isPassed());
        }

        @Test
        @DisplayName("07-J Results: Final decision reflects terminal status PASSED")
        void testGetProposalResults_terminalPassed() {
            Proposal proposal = createProposal(1004L, ProposalType.MAJOR_EXPENSE, ProposalStatus.PASSED);
            Vote vote1 = new Vote(1L, proposal, voter1, optApprove, new BigDecimal("80.00"), Instant.now());

            when(proposalRepository.findById(1004L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(1004L)).thenReturn(List.of(vote1));

            ProposalResultsResponse results = votingService.getProposalResults(1004L);

            assertNotNull(results);
            assertEquals(ProposalStatus.PASSED, results.getStatus());
            assertEquals("PASSED", results.getFinalDecision());
            assertEquals(new BigDecimal("75.00"), results.getThreshold());
            assertEquals(">= 75.00% of total eligible equity", results.getThresholdDescription());
        }

        @Test
        @DisplayName("07-J Results: Final decision reflects terminal status EXPIRED")
        void testGetProposalResults_terminalExpired() {
            Proposal proposal = createProposal(1005L, ProposalType.ROUTINE_EXPENSE, ProposalStatus.EXPIRED);

            when(proposalRepository.findById(1005L)).thenReturn(Optional.of(proposal));
            when(ownershipShareRepository.sumActivePercentagesByGroupId(sampleGroup.getId()))
                    .thenReturn(new BigDecimal("100.00"));
            when(voteRepository.findByProposalId(1005L)).thenReturn(List.of());

            ProposalResultsResponse results = votingService.getProposalResults(1005L);

            assertNotNull(results);
            assertEquals(ProposalStatus.EXPIRED, results.getStatus());
            assertEquals("EXPIRED", results.getFinalDecision());
        }
    }
}
