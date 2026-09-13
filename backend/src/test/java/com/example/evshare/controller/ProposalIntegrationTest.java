package com.example.evshare.controller;

import com.example.evshare.dto.request.CastVoteRequest;
import com.example.evshare.dto.request.CreateProposalRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.entity.enums.ProposalType;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import com.example.evshare.entity.enums.VoteOptionKey;
import com.example.evshare.repository.*;
import com.example.evshare.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 07-B — Proposal Creation & Governance Deliberation Integration Tests")
class ProposalIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;

    private static final java.util.concurrent.atomic.AtomicLong PHONE_SEQ = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    private User majorOwner;
    private User minorOwner;
    private User outsiderUser;
    private User adminUser;
    private User exactOwner;
    private User inactiveOwner;
    private OwnershipGroup testGroup;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        majorOwner = createTestUser("major.owner." + UUID.randomUUID() + "@evshare.io", "Nguyen Major Owner", roleCoOwner);
        minorOwner = createTestUser("minor.owner." + UUID.randomUUID() + "@evshare.io", "Tran Minor Owner", roleCoOwner);
        outsiderUser = createTestUser("outsider." + UUID.randomUUID() + "@evshare.io", "Le Outsider", roleCoOwner);
        adminUser = createTestUser("admin." + UUID.randomUUID() + "@evshare.io", "Admin User", roleAdmin);
        exactOwner = createTestUser("exact.owner." + UUID.randomUUID() + "@evshare.io", "Pham Exact Owner", roleCoOwner);
        inactiveOwner = createTestUser("inactive.owner." + UUID.randomUUID() + "@evshare.io", "Hoang Inactive Owner", roleCoOwner);

        Vehicle vehicle = new Vehicle();
        vehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        vehicle.setLicensePlate("51H-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        vehicle.setModelName("VinFast VF8 Eco");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(90);
        vehicle.setOdometerKm(new BigDecimal("10000.00"));
        vehicle.setStallLocationCode("BAY-01");
        vehicle = vehicleRepository.saveAndFlush(vehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        testGroup.setVehicle(vehicle);
        testGroup.setFormationDate(LocalDate.of(2026, 1, 1));
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        // Major owner holds 80.00% (eligible >= 10%)
        OwnershipShare majorShare = new OwnershipShare();
        majorShare.setGroup(testGroup);
        majorShare.setUser(majorOwner);
        majorShare.setPercentage(new BigDecimal("80.00"));
        majorShare.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        majorShare.setIsActive(true);
        ownershipShareRepository.saveAndFlush(majorShare);

        // Minor owner holds 5.00% (ineligible < 10%)
        OwnershipShare minorShare = new OwnershipShare();
        minorShare.setGroup(testGroup);
        minorShare.setUser(minorOwner);
        minorShare.setPercentage(new BigDecimal("5.00"));
        minorShare.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        minorShare.setIsActive(true);
        ownershipShareRepository.saveAndFlush(minorShare);

        // Exact owner holds 10.00% (eligible exactly 10%)
        OwnershipShare exactShare = new OwnershipShare();
        exactShare.setGroup(testGroup);
        exactShare.setUser(exactOwner);
        exactShare.setPercentage(new BigDecimal("10.00"));
        exactShare.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        exactShare.setIsActive(true);
        ownershipShareRepository.saveAndFlush(exactShare);

        // Inactive owner holds 20.00% but isActive = false
        OwnershipShare inactiveShare = new OwnershipShare();
        inactiveShare.setGroup(testGroup);
        inactiveShare.setUser(inactiveOwner);
        inactiveShare.setPercentage(new BigDecimal("20.00"));
        inactiveShare.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        inactiveShare.setIsActive(false);
        ownershipShareRepository.saveAndFlush(inactiveShare);
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyPasswordHashForSessionTesting12345");
        u.setFullName(fullName);
        u.setPhoneNumber(String.format("09%08d", PHONE_SEQ.incrementAndGet() % 100000000L));
        u.setIsActive(true);
        u.getRoles().add(role);
        return userRepository.saveAndFlush(u);
    }

    @Test
    @DisplayName("POST /api/v1/proposals: Eligible co-owner (80% stake) creates ROUTINE_EXPENSE proposal with 3 seeded options")
    void testCreateProposal_Success_EligibleCoOwner() throws Exception {
        CreateProposalRequest request = new CreateProposalRequest(
                testGroup.getId(),
                "Preventive Battery Diagnostic Service",
                "Scheduled 40,000km battery diagnostic and coolant flush.",
                ProposalType.ROUTINE_EXPENSE,
                null
        );

        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Preventive Battery Diagnostic Service")))
                .andExpect(jsonPath("$.data.proposalType", is("ROUTINE_EXPENSE")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.proposerUserId", is(majorOwner.getId().intValue())))
                .andExpect(jsonPath("$.data.groupId", is(testGroup.getId().intValue())))
                .andExpect(jsonPath("$.data.options", hasSize(3)))
                .andExpect(jsonPath("$.data.options[*].optionKey", containsInAnyOrder("APPROVE", "REJECT", "ABSTAIN")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals: Rejects creation when co-owner holds < 10% equity stake (BR-VOT-01)")
    void testCreateProposal_Forbidden_IneligibleMinorCoOwner() throws Exception {
        CreateProposalRequest request = new CreateProposalRequest(
                testGroup.getId(),
                "Proposal by Ineligible Member",
                "Minor member with only 5% equity attempting to sponsor proposal.",
                ProposalType.ROUTINE_EXPENSE,
                null
        );

        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(minorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("at least 10.00% active equity")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals: Rejects creation when user is not a syndicate member")
    void testCreateProposal_Forbidden_OutsiderUser() throws Exception {
        CreateProposalRequest request = new CreateProposalRequest(
                testGroup.getId(),
                "Outsider Proposal",
                "Outsider trying to initiate proposal in foreign group.",
                ProposalType.ROUTINE_EXPENSE,
                null
        );

        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(outsiderUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not an active co-owner")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals: Rejects proposal with blank title")
    void testCreateProposal_BadRequest_BlankTitle() throws Exception {
        CreateProposalRequest request = new CreateProposalRequest(
                testGroup.getId(),
                "",
                "Description with missing title",
                ProposalType.ROUTINE_EXPENSE,
                null
        );

        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/proposals/{id}: Co-owner retrieves proposal details and options")
    void testGetProposalById_Success() throws Exception {
        Proposal proposal = new Proposal();
        proposal.setGroup(testGroup);
        proposal.setProposerUser(majorOwner);
        proposal.setTitle("Major Upgrade Proposal");
        proposal.setDescription("Upgrade inverter system");
        proposal.setProposalType(ProposalType.MAJOR_EXPENSE);
        proposal.setStatus(ProposalStatus.ACTIVE);
        proposal.setVotingDeadline(java.time.Instant.now().plus(72, java.time.temporal.ChronoUnit.HOURS));
        proposal = proposalRepository.saveAndFlush(proposal);

        VoteOption opt1 = new VoteOption(null, proposal, com.example.evshare.entity.enums.VoteOptionKey.APPROVE, "Approve");
        VoteOption opt2 = new VoteOption(null, proposal, com.example.evshare.entity.enums.VoteOptionKey.REJECT, "Reject");
        VoteOption opt3 = new VoteOption(null, proposal, com.example.evshare.entity.enums.VoteOptionKey.ABSTAIN, "Abstain");
        voteOptionRepository.saveAllAndFlush(java.util.List.of(opt1, opt2, opt3));

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId())
                        .with(user(UserPrincipal.create(majorOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(proposal.getId().intValue())))
                .andExpect(jsonPath("$.data.title", is("Major Upgrade Proposal")))
                .andExpect(jsonPath("$.data.options", hasSize(3)));
    }

    @Test
    @DisplayName("GET /api/v1/proposals/group/{groupId}: Lists proposals for the group")
    void testGetProposalsByGroupId_Success() throws Exception {
        Proposal p1 = new Proposal();
        p1.setGroup(testGroup);
        p1.setProposerUser(majorOwner);
        p1.setTitle("Group Proposal 1");
        p1.setDescription("Description 1");
        p1.setProposalType(ProposalType.OPERATIONAL_RULE_CHANGE);
        p1.setStatus(ProposalStatus.ACTIVE);
        p1.setVotingDeadline(java.time.Instant.now().plus(72, java.time.temporal.ChronoUnit.HOURS));
        proposalRepository.saveAndFlush(p1);

        mockMvc.perform(get("/api/v1/proposals/group/" + testGroup.getId())
                        .with(user(UserPrincipal.create(majorOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/transition: Admin transitions proposal from ACTIVE to PASSED")
    void testTransitionProposalStatus_Success_ToPassed() throws Exception {
        Proposal proposal = createActiveProposal("Passable Proposal");

        com.example.evshare.dto.request.TransitionProposalStatusRequest request =
                new com.example.evshare.dto.request.TransitionProposalStatusRequest(
                        ProposalStatus.PASSED, "Quorum verified and approval threshold satisfied"
                );

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PASSED")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/transition: Admin transitions proposal from ACTIVE to REJECTED")
    void testTransitionProposalStatus_Success_ToRejected() throws Exception {
        Proposal proposal = createActiveProposal("Rejectable Proposal");

        com.example.evshare.dto.request.TransitionProposalStatusRequest request =
                new com.example.evshare.dto.request.TransitionProposalStatusRequest(
                        ProposalStatus.REJECTED, "Failed to achieve minimum approval threshold"
                );

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("REJECTED")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/transition: Admin transitions proposal from ACTIVE to EXPIRED")
    void testTransitionProposalStatus_Success_ToExpired() throws Exception {
        Proposal proposal = createActiveProposal("Expirable Proposal");

        com.example.evshare.dto.request.TransitionProposalStatusRequest request =
                new com.example.evshare.dto.request.TransitionProposalStatusRequest(
                        ProposalStatus.EXPIRED, "Voting window concluded without quorum"
                );

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("EXPIRED")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/transition: Rejects invalid transition from terminal PASSED status with 409 Conflict")
    void testTransitionProposalStatus_Conflict_FromTerminalStatus() throws Exception {
        Proposal proposal = createActiveProposal("Terminal Proposal");
        proposal.setStatus(ProposalStatus.PASSED);
        proposal = proposalRepository.saveAndFlush(proposal);

        com.example.evshare.dto.request.TransitionProposalStatusRequest request =
                new com.example.evshare.dto.request.TransitionProposalStatusRequest(
                        ProposalStatus.ACTIVE, "Attempting to revive passed proposal"
                );

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot transition proposal")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/transition: Rejects non-admin user with 403 Forbidden")
    void testTransitionProposalStatus_Forbidden_NonAdmin() throws Exception {
        Proposal proposal = createActiveProposal("Restricted Transition Proposal");

        com.example.evshare.dto.request.TransitionProposalStatusRequest request =
                new com.example.evshare.dto.request.TransitionProposalStatusRequest(
                        ProposalStatus.PASSED, "Unauthorized transition"
                );

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/transition")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/proposals/{id}/history: Retrieves audit transition history")
    void testGetProposalHistory_Success() throws Exception {
        Proposal proposal = createActiveProposal("Audited Proposal");

        // Transition proposal to generate audit entry
        com.example.evshare.dto.request.TransitionProposalStatusRequest request =
                new com.example.evshare.dto.request.TransitionProposalStatusRequest(
                        ProposalStatus.PASSED, "Quorum verified"
                );

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/history")
                        .with(user(UserPrincipal.create(majorOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].action", is("PROPOSAL_STATE_TRANSITION")))
                .andExpect(jsonPath("$.data[0].newStateJson", containsString("PASSED")));
    }

    private Proposal createActiveProposal(String title) {
        Proposal proposal = new Proposal();
        proposal.setGroup(testGroup);
        proposal.setProposerUser(majorOwner);
        proposal.setTitle(title);
        proposal.setDescription("Description for " + title);
        proposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
        proposal.setStatus(ProposalStatus.ACTIVE);
        proposal.setVotingDeadline(java.time.Instant.now().plus(72, java.time.temporal.ChronoUnit.HOURS));
        proposal = proposalRepository.saveAndFlush(proposal);

        VoteOption opt1 = new VoteOption(null, proposal, VoteOptionKey.APPROVE, "Approve");
        VoteOption opt2 = new VoteOption(null, proposal, VoteOptionKey.REJECT, "Reject");
        VoteOption opt3 = new VoteOption(null, proposal, VoteOptionKey.ABSTAIN, "Abstain");
        voteOptionRepository.saveAllAndFlush(java.util.List.of(opt1, opt2, opt3));

        return proposal;
    }

    @Test
    @DisplayName("POST /api/v1/proposals: Permitted when co-owner holds exactly 10.00% active equity (BR-VOT-01)")
    void testCreateProposal_Success_Exact10PercentOwner() throws Exception {
        CreateProposalRequest request = new CreateProposalRequest(
                testGroup.getId(),
                "Boundary 10% Equity Proposal",
                "Proposal initiated by owner with exact 10.00% equity stake.",
                ProposalType.ROUTINE_EXPENSE,
                null
        );

        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(exactOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.proposerUserId", is(exactOwner.getId().intValue())))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals: Rejects creation when owner is inactive (isActive = false)")
    void testCreateProposal_Forbidden_InactiveOwner() throws Exception {
        CreateProposalRequest request = new CreateProposalRequest(
                testGroup.getId(),
                "Inactive Owner Proposal",
                "Owner with 20% equity but isActive=false attempting to initiate proposal.",
                ProposalType.ROUTINE_EXPENSE,
                null
        );

        mockMvc.perform(post("/api/v1/proposals")
                        .with(user(UserPrincipal.create(inactiveOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("inactive co-owner")));
    }

    @Test
    @DisplayName("GET /api/v1/proposals/group/{groupId}/eligibility: Returns eligible=true for major owner (80%)")
    void testGetEligibility_EligibleMajorOwner() throws Exception {
        mockMvc.perform(get("/api/v1/proposals/group/" + testGroup.getId() + "/eligibility")
                        .with(user(UserPrincipal.create(majorOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.eligible", is(true)))
                .andExpect(jsonPath("$.data.groupMember", is(true)))
                .andExpect(jsonPath("$.data.activeMember", is(true)))
                .andExpect(jsonPath("$.data.equityPercentage", is(80.0)))
                .andExpect(jsonPath("$.data.requiredPercentage", is(10.0)));
    }

    @Test
    @DisplayName("GET /api/v1/proposals/group/{groupId}/eligibility: Returns eligible=true for exact 10% owner")
    void testGetEligibility_EligibleExactOwner() throws Exception {
        mockMvc.perform(get("/api/v1/proposals/group/" + testGroup.getId() + "/eligibility")
                        .with(user(UserPrincipal.create(exactOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.eligible", is(true)))
                .andExpect(jsonPath("$.data.equityPercentage", is(10.0)));
    }

    @Test
    @DisplayName("GET /api/v1/proposals/group/{groupId}/eligibility: Returns eligible=false for minor owner (< 10%)")
    void testGetEligibility_IneligibleMinorOwner() throws Exception {
        mockMvc.perform(get("/api/v1/proposals/group/" + testGroup.getId() + "/eligibility")
                        .with(user(UserPrincipal.create(minorOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.eligible", is(false)))
                .andExpect(jsonPath("$.data.groupMember", is(true)))
                .andExpect(jsonPath("$.data.activeMember", is(true)))
                .andExpect(jsonPath("$.data.equityPercentage", is(5.0)))
                .andExpect(jsonPath("$.data.reason", containsString("below the required 10.00%")));
    }

    @Test
    @DisplayName("GET /api/v1/proposals/group/{groupId}/eligibility: Returns eligible=false for inactive owner")
    void testGetEligibility_IneligibleInactiveOwner() throws Exception {
        mockMvc.perform(get("/api/v1/proposals/group/" + testGroup.getId() + "/eligibility")
                        .with(user(UserPrincipal.create(inactiveOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.eligible", is(false)))
                .andExpect(jsonPath("$.data.groupMember", is(true)))
                .andExpect(jsonPath("$.data.activeMember", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("inactive co-owner")));
    }

    @Test
    @DisplayName("GET /api/v1/proposals/group/{groupId}/eligibility: Returns eligible=false for non-owner outsider")
    void testGetEligibility_IneligibleNonOwner() throws Exception {
        mockMvc.perform(get("/api/v1/proposals/group/" + testGroup.getId() + "/eligibility")
                        .with(user(UserPrincipal.create(outsiderUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.eligible", is(false)))
                .andExpect(jsonPath("$.data.groupMember", is(false)))
                .andExpect(jsonPath("$.data.reason", containsString("not an active co-owner")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/votes: Major owner (80%) casts APPROVE ballot")
    void testCastVote_Success_Approve() throws Exception {
        Proposal proposal = createActiveProposal("Energy Storage Overhaul");
        CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.optionKey", is("APPROVE")))
                .andExpect(jsonPath("$.data.equityWeight", is(80.0)))
                .andExpect(jsonPath("$.data.userId", is(majorOwner.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/votes: Exact owner (10%) casts REJECT ballot")
    void testCastVote_Success_Reject() throws Exception {
        Proposal proposal = createActiveProposal("Suspension Tuning");
        CastVoteRequest request = new CastVoteRequest(VoteOptionKey.REJECT);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(exactOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.optionKey", is("REJECT")))
                .andExpect(jsonPath("$.data.equityWeight", is(10.0)))
                .andExpect(jsonPath("$.data.userId", is(exactOwner.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/votes: Minor owner (5%) casts ABSTAIN ballot")
    void testCastVote_Success_Abstain() throws Exception {
        Proposal proposal = createActiveProposal("Fleet Software Update");
        CastVoteRequest request = new CastVoteRequest(VoteOptionKey.ABSTAIN);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(minorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.optionKey", is("ABSTAIN")))
                .andExpect(jsonPath("$.data.equityWeight", is(5.0)))
                .andExpect(jsonPath("$.data.userId", is(minorOwner.getId().intValue())));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/votes: Rejects duplicate vote from same voter with HTTP 409 Conflict")
    void testCastVote_Conflict_DuplicateVote() throws Exception {
        Proposal proposal = createActiveProposal("Dual Vote Rejection");
        CastVoteRequest request1 = new CastVoteRequest(VoteOptionKey.APPROVE);
        CastVoteRequest request2 = new CastVoteRequest(VoteOptionKey.REJECT);

        // First ballot succeeds
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Duplicate ballot rejected
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already cast a ballot")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/votes: Rejects vote from non-owner outsider with HTTP 403 Forbidden")
    void testCastVote_Forbidden_NonOwner() throws Exception {
        Proposal proposal = createActiveProposal("Outsider Ballot Attempt");
        CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(outsiderUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not an active co-owner")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/votes: Rejects vote from inactive owner with HTTP 403 Forbidden")
    void testCastVote_Forbidden_InactiveOwner() throws Exception {
        Proposal proposal = createActiveProposal("Inactive Owner Ballot Attempt");
        CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(inactiveOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("inactive co-owner")));
    }

    @Test
    @DisplayName("POST /api/v1/proposals/{id}/votes: Rejects vote on terminal/non-ACTIVE proposal with HTTP 400 Bad Request")
    void testCastVote_BadRequest_NonActiveProposal() throws Exception {
        Proposal proposal = createActiveProposal("Terminal Proposal Ballot Attempt");
        proposal.setStatus(ProposalStatus.PASSED);
        proposalRepository.saveAndFlush(proposal);

        CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ACTIVE")));
    }

    @Test
    @DisplayName("GET /api/v1/proposals/{id}/votes: Retrieves cast ballots and my-vote")
    void testGetVotes_And_MyVote() throws Exception {
        Proposal proposal = createActiveProposal("Audit Votes Retrieval");
        CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get all votes for proposal
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].optionKey", is("APPROVE")));

        // Get my-vote
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/votes/my-vote")
                        .with(user(UserPrincipal.create(majorOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.optionKey", is("APPROVE")));
    }

    @Test
    @DisplayName("Duplicate Vote Prevention: Same voter can vote on different proposals in the same group")
    void testCastVote_DifferentProposals_Success() throws Exception {
        Proposal proposal1 = createActiveProposal("Proposal 1 - Maintenance");
        Proposal proposal2 = createActiveProposal("Proposal 2 - Charging Policy");

        CastVoteRequest request1 = new CastVoteRequest(VoteOptionKey.APPROVE);
        CastVoteRequest request2 = new CastVoteRequest(VoteOptionKey.REJECT);

        // Vote on proposal 1
        mockMvc.perform(post("/api/v1/proposals/" + proposal1.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proposalId", is(proposal1.getId().intValue())))
                .andExpect(jsonPath("$.data.optionKey", is("APPROVE")));

        // Vote on proposal 2
        mockMvc.perform(post("/api/v1/proposals/" + proposal2.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proposalId", is(proposal2.getId().intValue())))
                .andExpect(jsonPath("$.data.optionKey", is("REJECT")));
    }

    @Test
    @DisplayName("Duplicate Vote Prevention: Same voter can vote on proposals in different syndicate groups")
    void testCastVote_DifferentGroups_Success() throws Exception {
        // Create second vehicle and group
        Vehicle secondVehicle = new Vehicle();
        secondVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        secondVehicle.setLicensePlate("29A-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        secondVehicle.setModelName("VinFast VF9 Plus");
        secondVehicle.setManufacturer("VinFast");
        secondVehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        secondVehicle.setStatus(VehicleStatus.AVAILABLE);
        secondVehicle.setBatteryLevel(95);
        secondVehicle.setOdometerKm(new BigDecimal("5000.00"));
        secondVehicle.setStallLocationCode("BAY-02");
        secondVehicle = vehicleRepository.saveAndFlush(secondVehicle);

        OwnershipGroup secondGroup = new OwnershipGroup();
        secondGroup.setGroupName("Syndicate Beta " + UUID.randomUUID().toString().substring(0, 8));
        secondGroup.setVehicle(secondVehicle);
        secondGroup.setFormationDate(LocalDate.of(2026, 2, 1));
        secondGroup.setIsActive(true);
        secondGroup = ownershipGroupRepository.saveAndFlush(secondGroup);

        // Major owner also holds 40.00% active share in secondGroup
        OwnershipShare secondShare = new OwnershipShare();
        secondShare.setGroup(secondGroup);
        secondShare.setUser(majorOwner);
        secondShare.setPercentage(new BigDecimal("40.00"));
        secondShare.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        secondShare.setIsActive(true);
        ownershipShareRepository.saveAndFlush(secondShare);

        // Create proposal in Group 1
        Proposal group1Proposal = createActiveProposal("Group 1 Initiative");

        // Create proposal in Group 2
        Proposal group2Proposal = new Proposal();
        group2Proposal.setGroup(secondGroup);
        group2Proposal.setProposerUser(majorOwner);
        group2Proposal.setTitle("Group 2 Initiative");
        group2Proposal.setDescription("Group 2 governance proposal");
        group2Proposal.setProposalType(ProposalType.MAJOR_EXPENSE);
        group2Proposal.setStatus(ProposalStatus.ACTIVE);
        group2Proposal.setVotingDeadline(java.time.Instant.now().plus(72, java.time.temporal.ChronoUnit.HOURS));
        group2Proposal = proposalRepository.saveAndFlush(group2Proposal);

        VoteOption opt1 = new VoteOption(null, group2Proposal, VoteOptionKey.APPROVE, "Approve");
        VoteOption opt2 = new VoteOption(null, group2Proposal, VoteOptionKey.REJECT, "Reject");
        VoteOption opt3 = new VoteOption(null, group2Proposal, VoteOptionKey.ABSTAIN, "Abstain");
        voteOptionRepository.saveAllAndFlush(java.util.List.of(opt1, opt2, opt3));

        // Vote in Group 1: 80% equity weight
        CastVoteRequest req1 = new CastVoteRequest(VoteOptionKey.APPROVE);
        mockMvc.perform(post("/api/v1/proposals/" + group1Proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proposalId", is(group1Proposal.getId().intValue())))
                .andExpect(jsonPath("$.data.equityWeight", is(80.0)));

        // Vote in Group 2: 40% equity weight
        CastVoteRequest req2 = new CastVoteRequest(VoteOptionKey.APPROVE);
        mockMvc.perform(post("/api/v1/proposals/" + group2Proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(majorOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.proposalId", is(group2Proposal.getId().intValue())))
                .andExpect(jsonPath("$.data.equityWeight", is(40.0)));
    }

    @Test
    @DisplayName("07-G: 40/30/30 ownership - 40% APPROVE beats 30% REJECT (1-1 tie resolved by equity weight)")
    void testEquityWeightedVoting_40_30_30_Ownership() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("equity.a." + UUID.randomUUID() + "@evshare.io", "Equity Owner A (40%)", roleCoOwner);
        User ownerB = createTestUser("equity.b." + UUID.randomUUID() + "@evshare.io", "Equity Owner B (30%)", roleCoOwner);
        User ownerC = createTestUser("equity.c." + UUID.randomUUID() + "@evshare.io", "Equity Owner C (30%)", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("40-30-30 Syndicate");
        assignShare(group, ownerA, new BigDecimal("40.00"), true);
        assignShare(group, ownerB, new BigDecimal("30.00"), true);
        assignShare(group, ownerC, new BigDecimal("30.00"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "AC Filter Replacement", ProposalType.ROUTINE_EXPENSE);

        // Owner A (40%) votes APPROVE
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Owner B (30%) votes REJECT
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        // Owner C (30%) does not vote

        // Retrieve tally
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.proposalId", is(proposal.getId().intValue())))
                .andExpect(jsonPath("$.data.totalGroupActiveEquity", is(100.0)))
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(70.0)))
                .andExpect(jsonPath("$.data.totalVotersCount", is(2)))
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.approveEquity", is(40.0)))
                .andExpect(jsonPath("$.data.rejectEquity", is(30.0)))
                .andExpect(jsonPath("$.data.approvePercentageOfParticipating", is(57.14)))
                .andExpect(jsonPath("$.data.rejectPercentageOfParticipating", is(42.86)))
                .andExpect(jsonPath("$.data.passed", is(true)))
                .andExpect(jsonPath("$.data.ballots", hasSize(2)))
                .andExpect(jsonPath("$.data.ballots[0].userId", is(ownerA.getId().intValue())))
                .andExpect(jsonPath("$.data.ballots[0].equityWeight", is(40.0)))
                .andExpect(jsonPath("$.data.ballots[1].userId", is(ownerB.getId().intValue())))
                .andExpect(jsonPath("$.data.ballots[1].equityWeight", is(30.0)));
    }

    @Test
    @DisplayName("07-G: Uneven ownership (55.50 / 24.25 / 20.25) - 55.50% APPROVE beats two REJECTs (1 vs 2 headcount)")
    void testEquityWeightedVoting_UnevenOwnership_MajorityBeatsHeadcount() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("uneven.a." + UUID.randomUUID() + "@evshare.io", "Uneven Owner A (55.5%)", roleCoOwner);
        User ownerB = createTestUser("uneven.b." + UUID.randomUUID() + "@evshare.io", "Uneven Owner B (24.25%)", roleCoOwner);
        User ownerC = createTestUser("uneven.c." + UUID.randomUUID() + "@evshare.io", "Uneven Owner C (20.25%)", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Uneven Syndicate");
        assignShare(group, ownerA, new BigDecimal("55.50"), true);
        assignShare(group, ownerB, new BigDecimal("24.25"), true);
        assignShare(group, ownerC, new BigDecimal("20.25"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Tire Rotation", ProposalType.ROUTINE_EXPENSE);

        // Owner A (55.50%) votes APPROVE
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Owner B (24.25%) votes REJECT
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        // Owner C (20.25%) votes REJECT
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerC)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        // Retrieve tally
        // Headcount is 1 APPROVE vs 2 REJECT, but equity is 55.50% vs 44.50%
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(100.0)))
                .andExpect(jsonPath("$.data.totalVotersCount", is(3)))
                .andExpect(jsonPath("$.data.approveEquity", is(55.5)))
                .andExpect(jsonPath("$.data.rejectEquity", is(44.5)))
                .andExpect(jsonPath("$.data.approvePercentageOfParticipating", is(55.5)))
                .andExpect(jsonPath("$.data.passed", is(true)))
                .andExpect(jsonPath("$.data.ballots", hasSize(3)));
    }

    @Test
    @DisplayName("07-G: Inactive share is excluded from group active equity and cannot participate")
    void testEquityWeightedVoting_InactiveShareExcluded() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("inact.a." + UUID.randomUUID() + "@evshare.io", "Active Owner A (40%)", roleCoOwner);
        User ownerB = createTestUser("inact.b." + UUID.randomUUID() + "@evshare.io", "Active Owner B (30%)", roleCoOwner);
        User ownerC = createTestUser("inact.c." + UUID.randomUUID() + "@evshare.io", "Inactive Owner C (30%)", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Exclusion Syndicate");
        assignShare(group, ownerA, new BigDecimal("40.00"), true);
        assignShare(group, ownerB, new BigDecimal("30.00"), true);
        assignShare(group, ownerC, new BigDecimal("30.00"), false); // INACTIVE SHARE

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Brake Fluid Inspection", ProposalType.ROUTINE_EXPENSE);

        // Inactive Owner C attempts to vote -> 403 Forbidden
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerC)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("inactive co-owner")));

        // Active Owner A (40%) votes APPROVE
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Active Owner B (30%) votes APPROVE
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Retrieve tally
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalGroupActiveEquity", is(70.0))) // Inactive 30% excluded from active equity
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(70.0)))
                .andExpect(jsonPath("$.data.quorumReached", is(true))) // 70.00 >= 60.00
                .andExpect(jsonPath("$.data.approveEquity", is(70.0)))
                .andExpect(jsonPath("$.data.approvePercentageOfTotal", is(100.0))) // 70 / 70 = 100% of active equity
                .andExpect(jsonPath("$.data.passed", is(true)))
                .andExpect(jsonPath("$.data.ballots", hasSize(2)));
    }

    private OwnershipGroup createSyndicateGroup(String name) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        vehicle.setLicensePlate("30E-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        vehicle.setModelName("VinFast VF8 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setBatteryLevel(90);
        vehicle.setOdometerKm(new BigDecimal("12000.00"));
        vehicle.setStallLocationCode("BAY-05");
        vehicle = vehicleRepository.saveAndFlush(vehicle);

        OwnershipGroup group = new OwnershipGroup();
        group.setGroupName(name + " " + UUID.randomUUID().toString().substring(0, 8));
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.of(2026, 1, 15));
        group.setIsActive(true);
        return ownershipGroupRepository.saveAndFlush(group);
    }

    private OwnershipShare assignShare(OwnershipGroup group, User user, BigDecimal percentage, boolean isActive) {
        OwnershipShare share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(user);
        share.setPercentage(percentage);
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share.setIsActive(isActive);
        return ownershipShareRepository.saveAndFlush(share);
    }

    private Proposal createActiveProposalInGroup(OwnershipGroup group, User proposer, String title, ProposalType type) {
        Proposal proposal = new Proposal();
        proposal.setGroup(group);
        proposal.setProposerUser(proposer);
        proposal.setTitle(title);
        proposal.setDescription("Description for " + title);
        proposal.setProposalType(type);
        proposal.setStatus(ProposalStatus.ACTIVE);
        proposal.setVotingDeadline(java.time.Instant.now().plus(72, java.time.temporal.ChronoUnit.HOURS));
        proposal = proposalRepository.saveAndFlush(proposal);

        VoteOption opt1 = new VoteOption(null, proposal, VoteOptionKey.APPROVE, "Approve");
        VoteOption opt2 = new VoteOption(null, proposal, VoteOptionKey.REJECT, "Reject");
        VoteOption opt3 = new VoteOption(null, proposal, VoteOptionKey.ABSTAIN, "Abstain");
        voteOptionRepository.saveAllAndFlush(java.util.List.of(opt1, opt2, opt3));

        return proposal;
    }

    @Test
    @DisplayName("07-H Quorum: 59.99% participation strictly fails 60.00% quorum requirement")
    void testQuorum_59_99_Percent_FailsQuorum() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("q59.a." + UUID.randomUUID() + "@evshare.io", "Quorum Owner A (59.99%)", roleCoOwner);
        User ownerB = createTestUser("q59.b." + UUID.randomUUID() + "@evshare.io", "Quorum Owner B (40.01%)", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Quorum 59.99 Syndicate");
        assignShare(group, ownerA, new BigDecimal("59.99"), true);
        assignShare(group, ownerB, new BigDecimal("40.01"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Cabin Filter Inspection", ProposalType.ROUTINE_EXPENSE);

        // Only Owner A votes (59.99%)
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Tally check
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(59.99)))
                .andExpect(jsonPath("$.data.participationRatePercentage", is(59.99)))
                .andExpect(jsonPath("$.data.quorumReached", is(false)))
                .andExpect(jsonPath("$.data.passed", is(false)))
                .andExpect(jsonPath("$.data.outcomeReason", containsString("Quorum of 60.00% not reached")));
    }

    @Test
    @DisplayName("07-H Quorum: Exactly 60.00% participation satisfies quorum threshold")
    void testQuorum_Exactly_60_00_Percent_MeetsQuorum() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("q60.a." + UUID.randomUUID() + "@evshare.io", "Quorum Owner A (60.00%)", roleCoOwner);
        User ownerB = createTestUser("q60.b." + UUID.randomUUID() + "@evshare.io", "Quorum Owner B (40.00%)", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Quorum 60.00 Syndicate");
        assignShare(group, ownerA, new BigDecimal("60.00"), true);
        assignShare(group, ownerB, new BigDecimal("40.00"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Wiper Blade Replacements", ProposalType.ROUTINE_EXPENSE);

        // Owner A votes (60.00%)
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Tally check
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(60.0)))
                .andExpect(jsonPath("$.data.participationRatePercentage", is(60.0)))
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.passed", is(true)));
    }

    @Test
    @DisplayName("07-H Quorum: >60% participation (60.01%) satisfies quorum threshold")
    void testQuorum_GreaterThan_60_Percent_MeetsQuorum() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("q6001.a." + UUID.randomUUID() + "@evshare.io", "Quorum Owner A (60.01%)", roleCoOwner);
        User ownerB = createTestUser("q6001.b." + UUID.randomUUID() + "@evshare.io", "Quorum Owner B (39.99%)", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Quorum 60.01 Syndicate");
        assignShare(group, ownerA, new BigDecimal("60.01"), true);
        assignShare(group, ownerB, new BigDecimal("39.99"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Tire Rotation Maintenance", ProposalType.ROUTINE_EXPENSE);

        // Owner A votes (60.01%)
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Tally check
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(60.01)))
                .andExpect(jsonPath("$.data.participationRatePercentage", is(60.01)))
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.passed", is(true)));
    }

    @Test
    @DisplayName("07-H Quorum: ABSTAIN participation counts toward 60% quorum but does not inflate approval")
    void testQuorum_AbstainParticipation_MeetsQuorum() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("qabs.a." + UUID.randomUUID() + "@evshare.io", "Quorum Owner A (30%)", roleCoOwner);
        User ownerB = createTestUser("qabs.b." + UUID.randomUUID() + "@evshare.io", "Quorum Owner B (30%)", roleCoOwner);
        User ownerC = createTestUser("qabs.c." + UUID.randomUUID() + "@evshare.io", "Quorum Owner C (40%)", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Quorum Abstain Syndicate");
        assignShare(group, ownerA, new BigDecimal("30.00"), true);
        assignShare(group, ownerB, new BigDecimal("30.00"), true);
        assignShare(group, ownerC, new BigDecimal("40.00"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Dashcam Upgrade", ProposalType.ROUTINE_EXPENSE);

        // Owner A (30%) votes APPROVE
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Owner B (30%) votes ABSTAIN
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.ABSTAIN))))
                .andExpect(status().isCreated());

        // Tally check: 30% APPROVE + 30% ABSTAIN = 60.00% participating
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(60.0)))
                .andExpect(jsonPath("$.data.participationRatePercentage", is(60.0)))
                .andExpect(jsonPath("$.data.approveEquity", is(30.0)))
                .andExpect(jsonPath("$.data.abstainEquity", is(30.0)))
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                // Approval is 30/60 = 50.00%, strictly not > 50.00%, so passed = false
                .andExpect(jsonPath("$.data.passed", is(false)))
                .andExpect(jsonPath("$.data.outcomeReason", containsString("Did not achieve >50.00% approval")));
    }

    @Test
    @DisplayName("07-H Quorum: Zero votes cast results in 0% participation and fails quorum")
    void testQuorum_NoVotes_FailsQuorum() throws Exception {
        Proposal proposal = createActiveProposal("No Votes Quorum Proposal");

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(majorOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalVotersCount", is(0)))
                .andExpect(jsonPath("$.data.totalParticipatingEquity", is(0.0)))
                .andExpect(jsonPath("$.data.participationRatePercentage", is(0.0)))
                .andExpect(jsonPath("$.data.quorumReached", is(false)))
                .andExpect(jsonPath("$.data.passed", is(false)))
                .andExpect(jsonPath("$.data.outcomeReason", containsString("Quorum of 60.00% not reached")))
                .andExpect(jsonPath("$.data.ballots", hasSize(0)));
    }

    @Test
    @DisplayName("07-I ROUTINE Boundary: Exactly 50.00% approval fails (strictly > 50.00% required)")
    void testThreshold_Routine_Exactly50Percent_Fails() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("t50.a." + UUID.randomUUID() + "@evshare.io", "50% Owner A", roleCoOwner);
        User ownerB = createTestUser("t50.b." + UUID.randomUUID() + "@evshare.io", "50% Owner B", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Threshold 50.00 Syndicate");
        assignShare(group, ownerA, new BigDecimal("50.00"), true);
        assignShare(group, ownerB, new BigDecimal("50.00"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Routine Maintenance Split", ProposalType.ROUTINE_EXPENSE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.approvePercentageOfParticipating", is(50.0)))
                .andExpect(jsonPath("$.data.requiredThresholdPercentage", is(50.0)))
                .andExpect(jsonPath("$.data.thresholdType", is("RELATIVE_TO_PARTICIPATING")))
                .andExpect(jsonPath("$.data.passed", is(false)))
                .andExpect(jsonPath("$.data.outcomeReason", containsString("Did not achieve >50.00% approval")));
    }

    @Test
    @DisplayName("07-I ROUTINE Boundary: 50.01% approval passes (strictly > 50.00% met)")
    void testThreshold_Routine_50_01_Percent_Passes() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("t5001.a." + UUID.randomUUID() + "@evshare.io", "50.01% Owner A", roleCoOwner);
        User ownerB = createTestUser("t5001.b." + UUID.randomUUID() + "@evshare.io", "49.99% Owner B", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Threshold 50.01 Syndicate");
        assignShare(group, ownerA, new BigDecimal("50.01"), true);
        assignShare(group, ownerB, new BigDecimal("49.99"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Routine Maintenance Majority", ProposalType.ROUTINE_EXPENSE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.approvePercentageOfParticipating", is(50.01)))
                .andExpect(jsonPath("$.data.passed", is(true)))
                .andExpect(jsonPath("$.data.outcomeReason", containsString("Approved with 50.01%")));
    }

    @Test
    @DisplayName("07-I MAJOR Boundary: 74.99% approval fails (>= 75.00% supermajority required)")
    void testThreshold_Major_74_99_Percent_Fails() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("t7499.a." + UUID.randomUUID() + "@evshare.io", "74.99% Owner A", roleCoOwner);
        User ownerB = createTestUser("t7499.b." + UUID.randomUUID() + "@evshare.io", "25.01% Owner B", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Threshold 74.99 Syndicate");
        assignShare(group, ownerA, new BigDecimal("74.99"), true);
        assignShare(group, ownerB, new BigDecimal("25.01"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "High Voltage Inverter Replacement", ProposalType.MAJOR_EXPENSE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.approveEquity", is(74.99)))
                .andExpect(jsonPath("$.data.requiredThresholdPercentage", is(75.0)))
                .andExpect(jsonPath("$.data.thresholdType", is("RELATIVE_TO_TOTAL")))
                .andExpect(jsonPath("$.data.passed", is(false)))
                .andExpect(jsonPath("$.data.outcomeReason", containsString("Failed 75.00% supermajority threshold")));
    }

    @Test
    @DisplayName("07-I MAJOR Boundary: Exactly 75.00% approval passes (>= 75.00% inclusive boundary met)")
    void testThreshold_Major_Exactly75Percent_Passes() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("t75.a." + UUID.randomUUID() + "@evshare.io", "75% Owner A", roleCoOwner);
        User ownerB = createTestUser("t75.b." + UUID.randomUUID() + "@evshare.io", "25% Owner B", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Threshold 75.00 Syndicate");
        assignShare(group, ownerA, new BigDecimal("75.00"), true);
        assignShare(group, ownerB, new BigDecimal("25.00"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Battery Cell Module Swap", ProposalType.MAJOR_EXPENSE);

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                .andExpect(jsonPath("$.data.approveEquity", is(75.0)))
                .andExpect(jsonPath("$.data.passed", is(true)))
                .andExpect(jsonPath("$.data.outcomeReason", containsString("Supermajority satisfied with 75.00%")));
    }

    @Test
    @DisplayName("07-I Quorum First: 100% approval fails when quorum is not reached (< 60%)")
    void testThreshold_ApplyQuorumFirst_Integration() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("qfirst.a." + UUID.randomUUID() + "@evshare.io", "40% Owner A", roleCoOwner);
        User ownerB = createTestUser("qfirst.b." + UUID.randomUUID() + "@evshare.io", "60% Owner B", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Quorum First Syndicate");
        assignShare(group, ownerA, new BigDecimal("40.00"), true);
        assignShare(group, ownerB, new BigDecimal("60.00"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Unpopular Maintenance", ProposalType.ROUTINE_EXPENSE);

        // Owner A (40%) votes APPROVE. Owner B does not vote.
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Tally check: Quorum fails (40% < 60%), so passed = false despite 100% approval of participating
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/tally")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quorumReached", is(false)))
                .andExpect(jsonPath("$.data.approvePercentageOfParticipating", is(100.0)))
                .andExpect(jsonPath("$.data.passed", is(false)))
                .andExpect(jsonPath("$.data.outcomeReason", containsString("Quorum of 60.00% not reached")));
    }

    // ==========================================
    // 07-J: VOTING RESULTS TESTS (GET /api/v1/proposals/{id}/results)
    // ==========================================

    @Test
    @DisplayName("07-J Results: Returns all 8 required fields and does not expose individual ballots")
    void testProposalResults_Success_ReturnsAllRequiredFields() throws Exception {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER).orElseThrow();

        User ownerA = createTestUser("res.a." + UUID.randomUUID() + "@evshare.io", "Results Owner A (40%)", roleCoOwner);
        User ownerB = createTestUser("res.b." + UUID.randomUUID() + "@evshare.io", "Results Owner B (20%)", roleCoOwner);
        User ownerC = createTestUser("res.c." + UUID.randomUUID() + "@evshare.io", "Results Owner C (10%)", roleCoOwner);
        User ownerD = createTestUser("res.d." + UUID.randomUUID() + "@evshare.io", "Results Owner D (30%)", roleCoOwner);

        OwnershipGroup group = createSyndicateGroup("Results Test Syndicate");
        assignShare(group, ownerA, new BigDecimal("40.00"), true);
        assignShare(group, ownerB, new BigDecimal("20.00"), true);
        assignShare(group, ownerC, new BigDecimal("10.00"), true);
        assignShare(group, ownerD, new BigDecimal("30.00"), true);

        Proposal proposal = createActiveProposalInGroup(group, ownerA, "Battery Health Inspection Proposal", ProposalType.ROUTINE_EXPENSE);

        // Cast ballots:
        // Owner A (40%) APPROVE
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerA)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.APPROVE))))
                .andExpect(status().isCreated());

        // Owner B (20%) REJECT
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerB)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.REJECT))))
                .andExpect(status().isCreated());

        // Owner C (10%) ABSTAIN
        mockMvc.perform(post("/api/v1/proposals/" + proposal.getId() + "/votes")
                        .with(user(UserPrincipal.create(ownerC)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CastVoteRequest(VoteOptionKey.ABSTAIN))))
                .andExpect(status().isCreated());

        // Call GET /api/v1/proposals/{id}/results as Owner A
        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/results")
                        .with(user(UserPrincipal.create(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Proposal results retrieved successfully")))
                // 1. Total eligible equity
                .andExpect(jsonPath("$.data.totalEligibleEquity", is(100.0)))
                // 2. Participating equity (40 + 20 + 10 = 70.00%)
                .andExpect(jsonPath("$.data.participatingEquity", is(70.0)))
                // 3. Approve weight
                .andExpect(jsonPath("$.data.approveWeight", is(40.0)))
                // 4. Reject weight
                .andExpect(jsonPath("$.data.rejectWeight", is(20.0)))
                // 5. Abstain weight
                .andExpect(jsonPath("$.data.abstainWeight", is(10.0)))
                // 6. Quorum status (70.00% >= 60.00% -> REACHED)
                .andExpect(jsonPath("$.data.quorumStatus", is("REACHED")))
                .andExpect(jsonPath("$.data.quorumReached", is(true)))
                // 7. Threshold
                .andExpect(jsonPath("$.data.threshold", is(50.0)))
                .andExpect(jsonPath("$.data.thresholdDescription", is("> 50.00% of participating equity")))
                // 8. Final decision (40/70 = 57.14% > 50.00% -> PASSED)
                .andExpect(jsonPath("$.data.finalDecision", is("PASSED")))
                .andExpect(jsonPath("$.data.passed", is(true)))
                // Data Privacy Enforcement: Individual ballots MUST NOT be exposed
                .andExpect(jsonPath("$.data.ballots").doesNotExist());
    }

    @Test
    @DisplayName("07-J Results Security: Unauthenticated request is rejected with 401 Unauthorized")
    void testProposalResults_Security_UnauthorizedWhenUnauthenticated() throws Exception {
        Proposal proposal = createActiveProposal("Results Auth Proposal");

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/results"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("07-J Results Security: Non-group member outsider is rejected with 403 Forbidden")
    void testProposalResults_Security_ForbiddenForNonGroupMember() throws Exception {
        Proposal proposal = createActiveProposal("Results Outsider Proposal");

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/results")
                        .with(user(UserPrincipal.create(outsiderUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("07-J Results Security: Inactive co-owner is rejected with 403 Forbidden")
    void testProposalResults_Security_ForbiddenForInactiveCoOwner() throws Exception {
        Proposal proposal = createActiveProposal("Results Inactive CoOwner Proposal");

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/results")
                        .with(user(UserPrincipal.create(inactiveOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("07-J Results Security: Admin access is granted (200 OK)")
    void testProposalResults_Security_AdminAccessGranted() throws Exception {
        Proposal proposal = createActiveProposal("Results Admin Proposal");

        mockMvc.perform(get("/api/v1/proposals/" + proposal.getId() + "/results")
                        .with(user(UserPrincipal.create(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalEligibleEquity").isNumber())
                .andExpect(jsonPath("$.data.participatingEquity").isNumber())
                .andExpect(jsonPath("$.data.approveWeight").isNumber())
                .andExpect(jsonPath("$.data.rejectWeight").isNumber())
                .andExpect(jsonPath("$.data.abstainWeight").isNumber())
                .andExpect(jsonPath("$.data.quorumStatus").isString())
                .andExpect(jsonPath("$.data.threshold").isNumber())
                .andExpect(jsonPath("$.data.finalDecision").isString());
    }
}
