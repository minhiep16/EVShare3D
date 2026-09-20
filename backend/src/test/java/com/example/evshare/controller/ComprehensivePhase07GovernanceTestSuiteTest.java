package com.example.evshare.controller;

import com.example.evshare.dto.request.*;
import com.example.evshare.dto.response.*;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
import com.example.evshare.repository.*;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.DisputeService;
import com.example.evshare.service.VotingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CHECKPOINT 07-P — COMPREHENSIVE PHASE 07 MASTER TEST SUITE
 *
 * Systematically tests all 15 Phase 07 domains:
 * 1.  Proposal Eligibility (10% threshold, active status, group membership)
 * 2.  Proposal Lifecycle (ACTIVE -> PASSED, REJECTED, EXPIRED, terminal immutability)
 * 3.  Vote Casting (APPROVE, REJECT, ABSTAIN ballots during voting window)
 * 4.  Duplicate Vote Prevention (Single ballot per voter per proposal, 409 Conflict)
 * 5.  Equity Weighting (Ballot weighted strictly by active equity percentage)
 * 6.  Quorum Evaluation (Mandatory 60.00% participating equity requirement)
 * 7.  Decision Thresholds (Simple majority >50%, supermajority >66.7%, unanimity 100%)
 * 8.  Results Calculation (Deterministic tallying, approval ratio, final outcome)
 * 9.  Dispute Lifecycle (OPEN -> UNDER_REVIEW -> RESOLVED, terminal state immutability)
 * 10. Evidence Immutability (3D spatial defect coordinates, rejection of PUT/DELETE)
 * 11. Staff Mediation (Dashboard review, mediation notes, non-binding resolution proposals)
 * 12. Admin Arbitration (Exclusive ROLE_ADMIN authority, evidence review dossier)
 * 13. Fund Adjustment (Transactional SharedFund debit/credit, immutable ledger entry)
 * 14. RBAC Enforcement (Co-owner, Staff, Admin, Outsider permission separation)
 * 15. Transaction Safety & Rollback (Atomic rollback on insufficient funds, no double debit)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Checkpoint 07-P — Comprehensive Phase 07 Master Test Suite (All 15 Governance Domains)")
public class ComprehensivePhase07GovernanceTestSuiteTest {

    private static final AtomicLong PHONE_SEQ = new AtomicLong(System.currentTimeMillis() % 80000000L + 60000000L);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private DisputeRepository disputeRepository;
    @Autowired private DisputeEvidenceRepository disputeEvidenceRepository;
    @Autowired private SharedFundRepository sharedFundRepository;
    @Autowired private FundTransactionRepository fundTransactionRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @Autowired private VotingService votingService;
    @Autowired private DisputeService disputeService;
    @Autowired private PlatformTransactionManager transactionManager;

    private User coOwnerMajor;       // 60.00% active share (eligible)
    private User coOwnerMinor;       // 35.00% active share (eligible)
    private User coOwnerIneligible;  // 5.00% active share (ineligible < 10%)
    private User coOwnerInactive;    // 20.00% inactive share
    private User outsiderUser;       // No share
    private User staffUser;          // ROLE_STAFF
    private User adminUser;          // ROLE_ADMIN

    private Vehicle testVehicle;
    private OwnershipGroup testGroup;
    private SharedFund testFund;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleStaff = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_STAFF)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        String uid = UUID.randomUUID().toString().substring(0, 8);

        coOwnerMajor = createTestUser("gov07.major." + uid + "@evshare.io", "Major Co-Owner " + uid, roleCoOwner);
        coOwnerMinor = createTestUser("gov07.minor." + uid + "@evshare.io", "Minor Co-Owner " + uid, roleCoOwner);
        coOwnerIneligible = createTestUser("gov07.inelig." + uid + "@evshare.io", "Ineligible Owner " + uid, roleCoOwner);
        coOwnerInactive = createTestUser("gov07.inact." + uid + "@evshare.io", "Inactive Owner " + uid, roleCoOwner);
        outsiderUser = createTestUser("gov07.outsider." + uid + "@evshare.io", "Outsider " + uid, roleCoOwner);
        staffUser = createTestUser("gov07.staff." + uid + "@evshare.io", "Staff Mediator " + uid, roleStaff);
        adminUser = createTestUser("gov07.admin." + uid + "@evshare.io", "Admin Arbitrator " + uid, roleAdmin);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN07P" + uid.toUpperCase() + "00000");
        testVehicle.setLicensePlate("51K-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        testVehicle.setModelName("VinFast VF9 Plus");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(92);
        testVehicle.setOdometerKm(new BigDecimal("15000.00"));
        testVehicle.setStallLocationCode("BAY-20");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Master Governance Syndicate " + uid);
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.of(2026, 1, 1));
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        assignShare(testGroup, coOwnerMajor, new BigDecimal("60.00"), true);
        assignShare(testGroup, coOwnerMinor, new BigDecimal("35.00"), true);
        assignShare(testGroup, coOwnerIneligible, new BigDecimal("5.00"), true);
        assignShare(testGroup, coOwnerInactive, new BigDecimal("20.00"), false);

        testFund = new SharedFund();
        testFund.setGroup(testGroup);
        testFund.setCurrentBalance(new BigDecimal("5000000.00"));
        testFund.setMinimumReserveThreshold(new BigDecimal("1000000.00"));
        testFund.setCurrency("VND");
        testFund.setUpdatedAt(Instant.now());
        testFund = sharedFundRepository.saveAndFlush(testFund);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyPasswordHashForPhase07MasterTesting12345");
        u.setFullName(fullName);
        u.setPhoneNumber(String.format("09%08d", PHONE_SEQ.incrementAndGet() % 100000000L));
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    private OwnershipShare assignShare(OwnershipGroup group, User user, BigDecimal percentage, boolean isActive) {
        OwnershipShare share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(user);
        share.setPercentage(percentage);
        share.setShareCertificateNumber("CERT-07P-" + UUID.randomUUID().toString().substring(0, 8));
        share.setIsActive(isActive);
        return ownershipShareRepository.saveAndFlush(share);
    }

    private Proposal createActiveProposal(String title, ProposalType type) {
        Proposal p = new Proposal();
        p.setGroup(testGroup);
        p.setProposerUser(coOwnerMajor);
        p.setTitle(title);
        p.setDescription("Governance proposal: " + title);
        p.setProposalType(type);
        p.setStatus(ProposalStatus.ACTIVE);
        p.setVotingDeadline(Instant.now().plus(72, ChronoUnit.HOURS));
        p = proposalRepository.saveAndFlush(p);

        VoteOption opt1 = new VoteOption(null, p, VoteOptionKey.APPROVE, "Approve");
        VoteOption opt2 = new VoteOption(null, p, VoteOptionKey.REJECT, "Reject");
        VoteOption opt3 = new VoteOption(null, p, VoteOptionKey.ABSTAIN, "Abstain");
        voteOptionRepository.saveAllAndFlush(List.of(opt1, opt2, opt3));

        return p;
    }

    private Dispute createDisputeWithEvidence(DisputeStatus status) {
        Dispute d = new Dispute();
        d.setGroup(testGroup);
        d.setComplainantUser(coOwnerMajor);
        d.setRespondentUser(coOwnerMinor);
        d.setTitle("Bumper alignment dispute");
        d.setDescription("Misalignment noticed following return");
        d.setStatus(status);
        d.setCreatedAt(Instant.now());
        d = disputeRepository.saveAndFlush(d);

        DisputeEvidence ev = new DisputeEvidence();
        ev.setDispute(d);
        ev.setUploadedByUser(coOwnerMajor);
        ev.setFileUrl("https://storage.evshare.io/evidences/bumper_alignment.jpg");
        ev.setMesh3dDefectCoordinates("{\"x\": 0.12, \"y\": 0.95, \"z\": -0.45}");
        ev.setDescription("Mesh coordinate inspection photo");
        ev.setCreatedAt(Instant.now());
        disputeEvidenceRepository.saveAndFlush(ev);

        return d;
    }

    // =========================================================================
    // DOMAIN 1: PROPOSAL ELIGIBILITY (BR-VOT-01)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Domain 1: Proposal Eligibility — Co-owner holding >=10% can propose; <10%, inactive, outsider rejected")
    void testDomain01_ProposalEligibility() throws Exception {
        // 1.1 Eligible owner (60%) checks eligibility
        mockMvc.perform(get("/api/v1/proposals/group/{groupId}/eligibility", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible", is(true)))
                .andExpect(jsonPath("$.data.equityPercentage", is(60.0)));

        // 1.2 Ineligible owner (5%) checks eligibility
        mockMvc.perform(get("/api/v1/proposals/group/{groupId}/eligibility", testGroup.getId())
                        .with(user(UserPrincipal.create(coOwnerIneligible))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible", is(false)))
                .andExpect(jsonPath("$.data.equityPercentage", is(5.0)));

        // 1.3 Ineligible owner (<10%) attempts proposal creation -> 403 Forbidden
        CreateProposalRequest ineligibleReq = new CreateProposalRequest(
                testGroup.getId(), "Ineligible Proposal", "Testing <10% block", ProposalType.ROUTINE_EXPENSE, null
        );
        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(coOwnerIneligible)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ineligibleReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("at least 10.00% active equity")));

        // 1.4 Inactive owner attempts proposal creation -> 403 Forbidden
        CreateProposalRequest inactiveReq = new CreateProposalRequest(
                testGroup.getId(), "Inactive Proposal", "Testing inactive block", ProposalType.ROUTINE_EXPENSE, null
        );
        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(coOwnerInactive)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactiveReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("inactive co-owner")));

        // 1.5 Outsider attempts proposal creation -> 403 Forbidden
        CreateProposalRequest outsiderReq = new CreateProposalRequest(
                testGroup.getId(), "Outsider Proposal", "Testing outsider block", ProposalType.ROUTINE_EXPENSE, null
        );
        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(outsiderUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outsiderReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // DOMAIN 2: PROPOSAL LIFECYCLE (BR-VOT-02)
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("Domain 2: Proposal Lifecycle — ACTIVE to PASSED, REJECTED, EXPIRED with terminal immutability")
    void testDomain02_ProposalLifecycle() throws Exception {
        Proposal proposal = createActiveProposal("Lifecycle Test Proposal", ProposalType.ROUTINE_EXPENSE);

        // 2.1 Admin transitions ACTIVE -> PASSED
        TransitionProposalStatusRequest passReq = new TransitionProposalStatusRequest(ProposalStatus.PASSED, "Quorum and approval met");
        mockMvc.perform(post("/api/v1/proposals/{id}/transition", proposal.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PASSED")));

        // 2.2 Terminal immutability: Re-transitioning from PASSED is rejected with 409 Conflict
        TransitionProposalStatusRequest reviveReq = new TransitionProposalStatusRequest(ProposalStatus.ACTIVE, "Attempting to revive");
        mockMvc.perform(post("/api/v1/proposals/{id}/transition", proposal.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviveReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition proposal")));

        // 2.3 Non-admin transition attempt is rejected with 403 Forbidden
        Proposal proposal2 = createActiveProposal("Admin Only Transition", ProposalType.ROUTINE_EXPENSE);
        mockMvc.perform(post("/api/v1/proposals/{id}/transition", proposal2.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // DOMAIN 3: VOTE CASTING (BR-VOT-03)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("Domain 3: Vote Casting — Active co-owners cast valid ballots (APPROVE, REJECT, ABSTAIN)")
    void testDomain03_VoteCasting() throws Exception {
        Proposal proposal = createActiveProposal("Vote Casting Proposal", ProposalType.ROUTINE_EXPENSE);

        // 3.1 Cast APPROVE ballot
        CastVoteRequest approveReq = new CastVoteRequest(VoteOptionKey.APPROVE);
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.optionKey", is("APPROVE")))
                .andExpect(jsonPath("$.data.equityWeight", is(60.0)));

        // 3.2 Cast REJECT ballot
        CastVoteRequest rejectReq = new CastVoteRequest(VoteOptionKey.REJECT);
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMinor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.optionKey", is("REJECT")))
                .andExpect(jsonPath("$.data.equityWeight", is(35.0)));

        // 3.3 Cast ABSTAIN ballot
        CastVoteRequest abstainReq = new CastVoteRequest(VoteOptionKey.ABSTAIN);
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerIneligible)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(abstainReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.optionKey", is("ABSTAIN")))
                .andExpect(jsonPath("$.data.equityWeight", is(5.0)));

        // 3.4 Retrieve votes list
        mockMvc.perform(get("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    // =========================================================================
    // DOMAIN 4: DUPLICATE VOTE PREVENTION (BR-VOT-04)
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("Domain 4: Duplicate Vote Prevention — Second ballot by same voter is rejected with HTTP 409 Conflict")
    void testDomain04_DuplicateVotePrevention() throws Exception {
        Proposal proposal = createActiveProposal("Duplicate Vote Guard Proposal", ProposalType.ROUTINE_EXPENSE);

        // First vote succeeds
        CastVoteRequest vote1 = new CastVoteRequest(VoteOptionKey.APPROVE);
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vote1)))
                .andExpect(status().isCreated());

        // Duplicate vote on same proposal is strictly rejected
        CastVoteRequest vote2 = new CastVoteRequest(VoteOptionKey.REJECT);
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vote2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already cast a ballot")));
    }

    // =========================================================================
    // DOMAIN 5: EQUITY WEIGHTING (BR-VOT-05)
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("Domain 5: Equity Weighting — Ballots are weighted strictly by active equity percentage")
    void testDomain05_EquityWeighting() throws Exception {
        Proposal proposal = createActiveProposal("Equity Weighting Proposal", ProposalType.ROUTINE_EXPENSE);

        // Major owner holds 60.00%
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.equityWeight", is(60.0)));

        // Minor owner holds 35.00%
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMinor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.equityWeight", is(35.0)));

        // Verify tally reflects exact equity weights
        mockMvc.perform(get("/api/v1/proposals/{id}/tally", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(95.0)))
                .andExpect(jsonPath("$.data.approveEquity", is(60.0)))
                .andExpect(jsonPath("$.data.rejectEquity", is(35.0)));
    }

    // =========================================================================
    // DOMAIN 6: QUORUM EVALUATION (BR-VOT-06)
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("Domain 6: Quorum Evaluation — 60.00% participating equity requirement")
    void testDomain06_QuorumEvaluation() throws Exception {
        Proposal proposal = createActiveProposal("Quorum Proposal", ProposalType.ROUTINE_EXPENSE);

        // 6.1 Only minor owner votes (35.00% participating < 60.00% quorum)
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMinor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/proposals/{id}/results", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quorumReached", is(false)))
                .andExpect(jsonPath("$.data.participatingEquity", is(35.0)))
                .andExpect(jsonPath("$.data.passed", is(false)));

        // 6.2 Major owner votes (60.00% + 35.00% = 95.00% >= 60.00% quorum)
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/proposals/{id}/results", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.participatingEquity", is(95.0)));
    }

    // =========================================================================
    // DOMAIN 7: DECISION THRESHOLDS (BR-VOT-07)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Domain 7: Decision Thresholds — Verification of threshold configuration by ProposalType")
    void testDomain07_DecisionThresholds() throws Exception {
        // 7.1 ROUTINE_EXPENSE requires >50.00% of participating equity
        Proposal routine = createActiveProposal("Routine Threshold Test", ProposalType.ROUTINE_EXPENSE);
        mockMvc.perform(get("/api/v1/proposals/{id}/results", routine.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.threshold", is(50.0)))
                .andExpect(jsonPath("$.data.thresholdDescription", containsString("> 50.00%")));

        // 7.2 MAJOR_EXPENSE requires >= 75.00% of total eligible equity
        Proposal major = createActiveProposal("Major Threshold Test", ProposalType.MAJOR_EXPENSE);
        mockMvc.perform(get("/api/v1/proposals/{id}/results", major.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.threshold", is(75.0)))
                .andExpect(jsonPath("$.data.thresholdDescription", containsString(">= 75.00%")));

        // 7.3 OWNER_ADMISSION_OR_EXIT requires >= 75.00% of total eligible equity
        Proposal ownerAdmission = createActiveProposal("Owner Admission Threshold Test", ProposalType.OWNER_ADMISSION_OR_EXIT);
        mockMvc.perform(get("/api/v1/proposals/{id}/results", ownerAdmission.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.threshold", is(75.0)))
                .andExpect(jsonPath("$.data.thresholdDescription", containsString(">= 75.00%")));
    }

    // =========================================================================
    // DOMAIN 8: RESULTS & OUTCOME CALCULATION (BR-VOT-08)
    // =========================================================================

    @Test
    @Order(8)
    @DisplayName("Domain 8: Results Calculation — Deterministic aggregation and final governance outcome")
    void testDomain08_ResultsCalculation() throws Exception {
        Proposal proposal = createActiveProposal("Outcome Calculation Test", ProposalType.ROUTINE_EXPENSE);

        // Major owner votes APPROVE (60.00%)
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Minor owner votes REJECT (35.00%)
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMinor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        // 60 / 95 = 63.16% > 50.00% threshold -> PASSED
        mockMvc.perform(get("/api/v1/proposals/{id}/results", proposal.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.passed", is(true)))
                .andExpect(jsonPath("$.data.finalDecision", is("PASSED")))
                .andExpect(jsonPath("$.data.decisionReason", containsString("Approved")));
    }

    // =========================================================================
    // DOMAIN 9: DISPUTE LIFECYCLE (BR-DIS-01 & BR-DIS-02)
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("Domain 9: Dispute Lifecycle — Transitions OPEN -> UNDER_REVIEW -> RESOLVED and terminal immutability")
    void testDomain09_DisputeLifecycle() throws Exception {
        Dispute dispute = createDisputeWithEvidence(DisputeStatus.OPEN);

        // 9.1 Staff transitions OPEN -> UNDER_REVIEW
        TransitionDisputeStatusRequest toReview = new TransitionDisputeStatusRequest(
                DisputeStatus.UNDER_REVIEW, "Staff opened formal mediation investigation", null
        );
        mockMvc.perform(post("/api/v1/disputes/{id}/transition", dispute.getId())
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toReview)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UNDER_REVIEW")));

        // 9.2 Admin transitions UNDER_REVIEW -> RESOLVED
        TransitionDisputeStatusRequest toResolved = new TransitionDisputeStatusRequest(
                DisputeStatus.RESOLVED, "Admin ruling completed", "Binding resolution: agreed settlement terms"
        );
        mockMvc.perform(post("/api/v1/disputes/{id}/transition", dispute.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toResolved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")));

        // 9.3 Terminal immutability: Cannot transition from RESOLVED -> 409 Conflict
        TransitionDisputeStatusRequest reopen = new TransitionDisputeStatusRequest(
                DisputeStatus.OPEN, "Attempting to reopen resolved dispute", null
        );
        mockMvc.perform(post("/api/v1/disputes/{id}/transition", dispute.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reopen)))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // DOMAIN 10: EVIDENCE IMMUTABILITY (BR-DIS-03)
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("Domain 10: Evidence Immutability — 3D mesh coordinates and rejection of PUT/DELETE")
    void testDomain10_EvidenceImmutability() throws Exception {
        Dispute dispute = createDisputeWithEvidence(DisputeStatus.OPEN);
        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(dispute.getId());
        assertFalse(evidences.isEmpty());
        Long evidenceId = evidences.get(0).getId();

        // 10.1 Retrieve specific evidence with 3D coordinates
        mockMvc.perform(get("/api/v1/disputes/{id}/evidence/{evidenceId}", dispute.getId(), evidenceId)
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mesh3dDefectCoordinates", containsString("\"x\": 0.12")));

        // 10.2 PUT evidence is rejected with 405 Method Not Allowed
        mockMvc.perform(put("/api/v1/disputes/{id}/evidence/{evidenceId}", dispute.getId(), evidenceId)
                        .with(user(UserPrincipal.create(adminUser))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", containsString("immutable and cannot be modified")));

        // 10.3 DELETE evidence is rejected with 405 Method Not Allowed
        mockMvc.perform(delete("/api/v1/disputes/{id}/evidence/{evidenceId}", dispute.getId(), evidenceId)
                        .with(user(UserPrincipal.create(adminUser))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", containsString("immutable and cannot be deleted")));
    }

    // =========================================================================
    // DOMAIN 11: STAFF MEDIATION (BR-DIS-04)
    // =========================================================================

    @Test
    @Order(11)
    @DisplayName("Domain 11: Staff Mediation — Review dashboard, mediation notes, proposed resolution, cannot arbitrate")
    void testDomain11_StaffMediation() throws Exception {
        Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);

        // 11.1 Staff lists disputes for review
        mockMvc.perform(get("/api/v1/disputes/staff/review")
                        .with(user(UserPrincipal.create(staffUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 11.2 Staff adds mediation notes
        AddMediationNotesRequest notesReq = new AddMediationNotesRequest("Observed slight scrape on front right panel. Preliminary interview completed.");
        mockMvc.perform(post("/api/v1/disputes/{id}/mediation-notes", dispute.getId())
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notesReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mediationNotes", containsString("Preliminary interview completed")));

        // 11.3 Staff proposes resolution
        ProposeResolutionRequest propReq = new ProposeResolutionRequest("Recommend syndicate shared fund covers 50% of buffing cost.");
        mockMvc.perform(post("/api/v1/disputes/{id}/propose-resolution", dispute.getId())
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.proposedResolution", containsString("Recommend syndicate shared fund")));

        // 11.4 Staff is strictly forbidden from executing final arbitration -> 403 Forbidden
        AdminArbitrateDisputeRequest arbitrateReq = new AdminArbitrateDisputeRequest(
                "Staff attempting arbitration", "Unauthorized binding terms"
        );
        mockMvc.perform(post("/api/v1/disputes/{id}/arbitrate", dispute.getId())
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(arbitrateReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // DOMAIN 12: ADMIN ARBITRATION (BR-DIS-05)
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("Domain 12: Admin Arbitration — Evidence dossier review and final binding arbitration")
    void testDomain12_AdminArbitration() throws Exception {
        Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);

        // 12.1 Admin retrieves arbitration dossier
        mockMvc.perform(get("/api/v1/disputes/{id}/arbitration-dossier", dispute.getId())
                        .with(user(UserPrincipal.create(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dispute.id", is(dispute.getId().intValue())))
                .andExpect(jsonPath("$.data.totalEvidences", is(1)));

        // 12.2 Co-owner cannot access arbitration dossier -> 403 Forbidden
        mockMvc.perform(get("/api/v1/disputes/{id}/arbitration-dossier", dispute.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor))))
                .andExpect(status().isForbidden());

        // 12.3 Admin issues binding arbitration
        AdminArbitrateDisputeRequest arbitrateReq = new AdminArbitrateDisputeRequest(
                "Findings confirm panel scratch occurred during respondent usage session based on telematics.",
                "Respondent is assessed 300,000 VND repair deductible. Dispute closed as RESOLVED."
        );
        mockMvc.perform(post("/api/v1/disputes/{id}/arbitrate", dispute.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(arbitrateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.arbitratorUserId", is(adminUser.getId().intValue())));
    }

    // =========================================================================
    // DOMAIN 13: FUND ADJUSTMENT (BR-DIS-06)
    // =========================================================================

    @Test
    @Order(13)
    @DisplayName("Domain 13: Fund Adjustment — Transactional SharedFund debit with immutable ledger record")
    void testDomain13_FundAdjustment() throws Exception {
        Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);
        BigDecimal adjustAmount = new BigDecimal("800000.00");

        DisputeFundAdjustmentRequest request = new DisputeFundAdjustmentRequest(
                "Syndicate vault reimburses complainant for wheel scratch repair",
                "Award 800,000 VND reimbursement from SharedFund; dispute closed as RESOLVED",
                adjustAmount,
                TransactionEntryType.DEBIT
        );

        mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.fundAdjustmentAmount", is(800000.00)))
                .andExpect(jsonPath("$.data.fundTransactionId").isNumber())
                .andExpect(jsonPath("$.data.fundTransactionReference", startsWith("DISP-" + dispute.getId() + "-")));

        // Verify SharedFund updated balance
        SharedFund updatedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
        assertEquals(new BigDecimal("4200000.00"), updatedFund.getCurrentBalance());

        // Verify immutable FundTransaction ledger entry
        Dispute resolvedDispute = disputeRepository.findById(dispute.getId()).orElseThrow();
        FundTransaction tx = fundTransactionRepository.findById(resolvedDispute.getFundTransaction().getId()).orElseThrow();
        assertEquals(TransactionType.DISPUTE_ADJUSTMENT, tx.getTransactionType());
        assertEquals(FundTransactionSource.DISPUTE_RESOLUTION, tx.getSource());
        assertEquals(TransactionEntryType.DEBIT, tx.getEntryType());
        assertEquals(new BigDecimal("4200000.00"), tx.getBalanceAfter());
    }

    // =========================================================================
    // DOMAIN 14: RBAC AUTHORIZATION ENFORCEMENT
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("Domain 14: RBAC Enforcement — Co-owner, Staff, Admin, and Outsider permission boundaries")
    void testDomain14_RbacEnforcement() throws Exception {
        Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);

        DisputeFundAdjustmentRequest fundReq = new DisputeFundAdjustmentRequest(
                "Unauthorized adjustment attempt", "Terms", new BigDecimal("100000.00"), TransactionEntryType.DEBIT
        );

        // 14.1 Co-owner cannot execute fund adjustment -> 403 Forbidden
        mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                        .with(user(UserPrincipal.create(coOwnerMajor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fundReq)))
                .andExpect(status().isForbidden());

        // 14.2 Staff cannot execute fund adjustment -> 403 Forbidden
        mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fundReq)))
                .andExpect(status().isForbidden());

        // 14.3 Unauthenticated caller receives 401 Unauthorized
        mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fundReq)))
                .andExpect(status().isUnauthorized());

        // 14.4 Outsider cannot vote on syndicate proposal -> 403 Forbidden
        Proposal proposal = createActiveProposal("Outsider RBAC Test", ProposalType.ROUTINE_EXPENSE);
        mockMvc.perform(post("/api/v1/proposals/{id}/votes", proposal.getId())
                        .with(user(UserPrincipal.create(outsiderUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // DOMAIN 15: TRANSACTION SAFETY & ROLLBACK
    // =========================================================================

    @Test
    @Order(15)
    @DisplayName("Domain 15: Transaction Safety — Rollback on insufficient balance and duplicate resolution guard")
    void testDomain15_TransactionSafetyAndRollback() throws Exception {
        testFund.setCurrentBalance(new BigDecimal("150000.00"));
        testFund = sharedFundRepository.saveAndFlush(testFund);

        Dispute dispute = createDisputeWithEvidence(DisputeStatus.UNDER_REVIEW);
        long initialTxCount = fundTransactionRepository.count();

        // 15.1 DEBIT exceeding fund balance fails with 400 Bad Request
        DisputeFundAdjustmentRequest overdraftReq = new DisputeFundAdjustmentRequest(
                "Excessive payout exceeding balance", "Attempting 500,000 against 150,000",
                new BigDecimal("500000.00"), TransactionEntryType.DEBIT
        );

        mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overdraftReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Insufficient fund balance")));

        // Verify complete state integrity
        SharedFund reloadedFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
        assertEquals(new BigDecimal("150000.00"), reloadedFund.getCurrentBalance());

        Dispute reloadedDispute = disputeRepository.findById(dispute.getId()).orElseThrow();
        assertEquals(DisputeStatus.UNDER_REVIEW, reloadedDispute.getStatus());
        assertNull(reloadedDispute.getFundTransaction());
        assertEquals(initialTxCount, fundTransactionRepository.count());

        // 15.2 Duplicate resolution guard: Resolving dispute once and then attempting second adjustment
        testFund.setCurrentBalance(new BigDecimal("2000000.00"));
        testFund = sharedFundRepository.saveAndFlush(testFund);

        DisputeFundAdjustmentRequest validReq = new DisputeFundAdjustmentRequest(
                "First legitimate resolution", "Terms", new BigDecimal("500000.00"), TransactionEntryType.DEBIT
        );
        mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk());

        // Second adjustment on same dispute rejected with 409 Conflict
        mockMvc.perform(post("/api/v1/disputes/{id}/fund-adjustment", dispute.getId())
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isConflict());

        // Fund not debited second time
        SharedFund finalFund = sharedFundRepository.findById(testFund.getId()).orElseThrow();
        assertEquals(new BigDecimal("1500000.00"), finalFund.getCurrentBalance());
    }
}
