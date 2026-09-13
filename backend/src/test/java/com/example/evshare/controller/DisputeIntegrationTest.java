package com.example.evshare.controller;

import com.example.evshare.dto.request.AddMediationNotesRequest;
import com.example.evshare.dto.request.AdminArbitrateDisputeRequest;
import com.example.evshare.dto.request.CreateDisputeEvidenceRequest;
import com.example.evshare.dto.request.CreateDisputeRequest;
import com.example.evshare.dto.request.ProposeResolutionRequest;
import com.example.evshare.dto.request.TransitionDisputeStatusRequest;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.DisputeStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Checkpoint 07-K & 07-L — Dispute Creation, Lifecycle & Evidence Integration Tests")
class DisputeIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private DisputeRepository disputeRepository;
    @Autowired private DisputeEvidenceRepository disputeEvidenceRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private static final AtomicLong PHONE_SEQ = new AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    private User activeOwner;
    private User respondentOwner;
    private User outsiderUser;
    private User inactiveOwner;
    private User staffUser;
    private User adminUser;
    private OwnershipGroup testGroup;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));
        Role roleStaff = roleRepository.findByName(RoleName.ROLE_STAFF)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_STAFF)));
        Role roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_ADMIN)));

        activeOwner = createTestUser("dispute.active." + UUID.randomUUID() + "@evshare.io", "Nguyen Active Owner", roleCoOwner);
        respondentOwner = createTestUser("dispute.resp." + UUID.randomUUID() + "@evshare.io", "Tran Respondent Owner", roleCoOwner);
        outsiderUser = createTestUser("dispute.outsider." + UUID.randomUUID() + "@evshare.io", "Le Outsider", roleCoOwner);
        inactiveOwner = createTestUser("dispute.inactive." + UUID.randomUUID() + "@evshare.io", "Hoang Inactive Owner", roleCoOwner);
        staffUser = createTestUser("dispute.staff." + UUID.randomUUID() + "@evshare.io", "Staff Reviewer", roleStaff);
        adminUser = createTestUser("dispute.admin." + UUID.randomUUID() + "@evshare.io", "Admin Operator", roleAdmin);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51H-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        testVehicle.setModelName("VinFast VF9 Plus");
        testVehicle.setManufacturer("VinFast");
        testVehicle.setModel3dAssetPath("models/vehicles/vf9.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(95);
        testVehicle.setOdometerKm(new BigDecimal("8500.00"));
        testVehicle.setStallLocationCode("BAY-10");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);

        testGroup = new OwnershipGroup();
        testGroup.setGroupName("Syndicate " + UUID.randomUUID().toString().substring(0, 8));
        testGroup.setVehicle(testVehicle);
        testGroup.setFormationDate(LocalDate.of(2026, 1, 1));
        testGroup.setIsActive(true);
        testGroup = ownershipGroupRepository.saveAndFlush(testGroup);

        // Active owner (50% active)
        assignShare(testGroup, activeOwner, new BigDecimal("50.00"), true);
        // Respondent owner (50% active)
        assignShare(testGroup, respondentOwner, new BigDecimal("50.00"), true);
        // Inactive owner (isActive = false)
        assignShare(testGroup, inactiveOwner, new BigDecimal("20.00"), false);
        // outsiderUser has no share
    }

    private User createTestUser(String email, String fullName, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("$2a$12$dummyPasswordHashForDisputeTesting12345");
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
        share.setShareCertificateNumber("CERT-" + UUID.randomUUID().toString().substring(0, 8));
        share.setIsActive(isActive);
        return ownershipShareRepository.saveAndFlush(share);
    }

    private CreateDisputeRequest createValidDisputePayload() {
        CreateDisputeEvidenceRequest ev = new CreateDisputeEvidenceRequest(
                "https://storage.evshare.io/evidences/scratch_dent.jpg",
                "{\"x\": 0.45, \"y\": 1.10, \"z\": -0.20, \"panel\": \"driver_door\"}",
                "Visible deep dent along the driver side door panel"
        );
        return new CreateDisputeRequest(
                testGroup.getId(),
                null,
                respondentOwner.getId(),
                "Unreported driver side door dent post check-out",
                "During pre-trip inspection, a dent was found on driver door that occurred during previous booking.",
                List.of(ev)
        );
    }

    @Test
    @DisplayName("POST /api/v1/disputes: Active co-owner files dispute with valid evidence (201 Created)")
    void testCreateDispute_Success_ActiveCoOwner() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();

        mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Dispute filed successfully")))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.groupId", is(testGroup.getId().intValue())))
                .andExpect(jsonPath("$.data.complainantUserId", is(activeOwner.getId().intValue())))
                .andExpect(jsonPath("$.data.respondentUserId", is(respondentOwner.getId().intValue())))
                .andExpect(jsonPath("$.data.title", is("Unreported driver side door dent post check-out")))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andExpect(jsonPath("$.data.evidences", hasSize(1)))
                .andExpect(jsonPath("$.data.evidences[0].fileUrl", is("https://storage.evshare.io/evidences/scratch_dent.jpg")));

        // Database assertions
        List<Dispute> disputes = disputeRepository.findByGroupId(testGroup.getId());
        assertEquals(1, disputes.size());
        Dispute d = disputes.get(0);
        assertEquals(DisputeStatus.OPEN, d.getStatus());
        assertEquals(activeOwner.getId(), d.getComplainantUser().getId());

        List<DisputeEvidence> evidences = disputeEvidenceRepository.findByDisputeId(d.getId());
        assertEquals(1, evidences.size());
        assertEquals("https://storage.evshare.io/evidences/scratch_dent.jpg", evidences.get(0).getFileUrl());
    }

    @Test
    @DisplayName("POST /api/v1/disputes: Outsider non-member is rejected with 403 Forbidden")
    void testCreateDispute_Security_ForbiddenForOutsider() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();

        mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(outsiderUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not a co-owner")));
    }

    @Test
    @DisplayName("POST /api/v1/disputes: Inactive co-owner is rejected with 403 Forbidden")
    void testCreateDispute_Security_ForbiddenForInactiveCoOwner() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();

        mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(inactiveOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("inactive co-owner")));
    }

    @Test
    @DisplayName("POST /api/v1/disputes: Unauthenticated request is rejected with 401 Unauthorized")
    void testCreateDispute_Security_UnauthorizedWhenUnauthenticated() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();

        mockMvc.perform(post("/api/v1/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/disputes: Empty evidence list violates evidence requirement (400 Bad Request)")
    void testCreateDispute_EvidenceRequirement_EmptyList_Rejected() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        request.setEvidences(List.of()); // Empty evidence

        mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/disputes: Blank reason description is rejected (400 Bad Request)")
    void testCreateDispute_ReasonValidation_BlankDescription_Rejected() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        request.setDescription("   "); // Blank

        mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/disputes: Self-dispute is rejected (400 Bad Request)")
    void testCreateDispute_RelatedEntity_SelfDispute_Rejected() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        request.setRespondentUserId(activeOwner.getId()); // Self

        mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot file a dispute against themselves")));
    }

    @Test
    @DisplayName("GET /api/v1/disputes/{id}: Details and evidence accessible by syndicate co-owner")
    void testGetDisputeById_Success_SyndicateMember() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();

        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        // Get as respondent co-owner (who belongs to group) -> 200 OK
        mockMvc.perform(get("/api/v1/disputes/" + disputeId)
                        .with(user(UserPrincipal.create(respondentOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(disputeId.intValue())))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andExpect(jsonPath("$.data.evidences", hasSize(1)));

        // Get as outsider -> 403 Forbidden
        mockMvc.perform(get("/api/v1/disputes/" + disputeId)
                        .with(user(UserPrincipal.create(outsiderUser))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/disputes/{id}/transition: Staff/Admin transitions lifecycle status")
    void testTransitionDisputeStatus_LifecycleWorkflow() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();

        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        // Step 1: Admin transitions OPEN -> UNDER_REVIEW
        TransitionDisputeStatusRequest req1 = new TransitionDisputeStatusRequest(
                DisputeStatus.UNDER_REVIEW, "Staff reviewing evidence", null
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UNDER_REVIEW")));

        // Step 2: Admin transitions UNDER_REVIEW -> RESOLVED
        TransitionDisputeStatusRequest req2 = new TransitionDisputeStatusRequest(
                DisputeStatus.RESOLVED, "Arbitration completed", "Reimbursed 400,000 VND from respondent fund"
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.resolutionSummary", is("Reimbursed 400,000 VND from respondent fund")));
    }

    @Test
    @DisplayName("POST /api/v1/disputes/{id}/evidence: Co-owner uploads supplementary evidence")
    void testAddEvidence_Success() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();

        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        CreateDisputeEvidenceRequest extraEv = new CreateDisputeEvidenceRequest(
                "https://storage.evshare.io/evidences/receipt_repair.pdf",
                null,
                "Authorized garage repair cost quotation"
        );

        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/evidence")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraEv)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fileUrl", is("https://storage.evshare.io/evidences/receipt_repair.pdf")));

        // Verify total evidences is now 2
        mockMvc.perform(get("/api/v1/disputes/" + disputeId)
                        .with(user(UserPrincipal.create(activeOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidences", hasSize(2)));

        // Verify AuditLog was persisted for evidence attachment
        List<AuditLog> logs = auditLogRepository.findByEntityNameAndEntityId("DisputeEvidence", disputeId);
        // Note: entityId is evidence ID
        List<AuditLog> allEvidenceLogs = auditLogRepository.findAll().stream()
                .filter(l -> "DisputeEvidence".equals(l.getEntityName()))
                .toList();
        assertTrue(allEvidenceLogs.size() >= 2, "Both initial and supplementary evidence should have audit log entries");
    }

    @Test
    @DisplayName("POST /api/v1/disputes/{id}/evidence: Outsider non-member is rejected with 403 Forbidden")
    void testAddEvidence_Security_OutsiderForbidden() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        CreateDisputeEvidenceRequest extraEv = new CreateDisputeEvidenceRequest(
                "https://storage.evshare.io/evidences/outsider.pdf", null, "Outsider note"
        );

        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/evidence")
                        .with(user(UserPrincipal.create(outsiderUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraEv)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/disputes/{id}/evidence: Inactive co-owner is rejected with 403 Forbidden")
    void testAddEvidence_Security_InactiveCoOwnerForbidden() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        CreateDisputeEvidenceRequest extraEv = new CreateDisputeEvidenceRequest(
                "https://storage.evshare.io/evidences/inactive.pdf", null, "Inactive owner note"
        );

        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/evidence")
                        .with(user(UserPrincipal.create(inactiveOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraEv)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/disputes/{id}/evidence: Attaching evidence to RESOLVED dispute is rejected with 409 Conflict")
    void testAddEvidence_ConflictWhenDisputeResolved() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        // Admin resolves dispute
        TransitionDisputeStatusRequest resolveReq = new TransitionDisputeStatusRequest(
                DisputeStatus.RESOLVED, "Resolved amicably", "Settled"
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")));

        // Attempt to upload supplementary evidence after resolution
        CreateDisputeEvidenceRequest extraEv = new CreateDisputeEvidenceRequest(
                "https://storage.evshare.io/evidences/too_late.jpg", null, "Late submission"
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/evidence")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraEv)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot attach evidence to a RESOLVED dispute")));
    }

    @Test
    @DisplayName("GET /api/v1/disputes/{id}/evidence/{evidenceId}: Co-owner retrieves specific evidence record")
    void testGetDisputeEvidenceById_Success() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();
        Long evidenceId = objectMapper.readTree(resJson).path("data").path("evidences").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/disputes/" + disputeId + "/evidence/" + evidenceId)
                        .with(user(UserPrincipal.create(activeOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(evidenceId.intValue())))
                .andExpect(jsonPath("$.data.fileUrl", is("https://storage.evshare.io/evidences/scratch_dent.jpg")))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/disputes/{id}/history: Co-owner views complete audit trail history")
    void testGetDisputeHistory_Success() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/disputes/" + disputeId + "/history")
                        .with(user(UserPrincipal.create(activeOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(not(empty()))))
                .andExpect(jsonPath("$.data[*].action", hasItems("DISPUTE_CREATED", "DISPUTE_EVIDENCE_ATTACHED")));
    }

    @Test
    @DisplayName("PUT & DELETE /api/v1/disputes/{id}/evidence/{evidenceId}: Modifications rejected (405 Method Not Allowed)")
    void testEvidenceImmutability_ModificationRejected() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();
        Long evidenceId = objectMapper.readTree(resJson).path("data").path("evidences").get(0).path("id").asLong();

        // PUT is rejected
        mockMvc.perform(put("/api/v1/disputes/" + disputeId + "/evidence/" + evidenceId)
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileUrl\": \"https://tampered.url/new.jpg\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", containsString("Dispute evidence is immutable and cannot be modified")));

        // DELETE is rejected
        mockMvc.perform(delete("/api/v1/disputes/" + disputeId + "/evidence/" + evidenceId)
                        .with(user(UserPrincipal.create(activeOwner))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message", containsString("Dispute evidence is immutable and cannot be deleted")));
    }

    @Test
    @DisplayName("GET /api/v1/disputes/staff/review: Staff views pending disputes (200 OK); Co-owner rejected (403 Forbidden)")
    void testStaffReview_ListDisputes_RBAC() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Staff access -> 200 OK
        mockMvc.perform(get("/api/v1/disputes/staff/review")
                        .with(user(UserPrincipal.create(staffUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(not(empty()))));

        // Co-owner access -> 403 Forbidden
        mockMvc.perform(get("/api/v1/disputes/staff/review")
                        .with(user(UserPrincipal.create(activeOwner))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/disputes/{id}/mediation-notes: Staff adds notes (200 OK); Co-owner rejected (403 Forbidden)")
    void testAddMediationNotes_RBAC() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        AddMediationNotesRequest notesReq = new AddMediationNotesRequest(
                "Interviewed respondent co-owner. Damage verified against check-in telemetry."
        );

        // Staff records notes -> 200 OK
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/mediation-notes")
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notesReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.mediationNotes", containsString("Interviewed respondent co-owner")))
                .andExpect(jsonPath("$.data.mediatorUserId", is(staffUser.getId().intValue())));

        // Co-owner records notes -> 403 Forbidden
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/mediation-notes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notesReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/disputes/{id}/propose-resolution: Staff proposes resolution (200 OK); Co-owner rejected (403 Forbidden)")
    void testProposeResolution_RBAC() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        ProposeResolutionRequest propReq = new ProposeResolutionRequest(
                "Staff proposal: 50% split of repair cost between driver and maintenance reserve."
        );

        // Staff proposes resolution -> 200 OK
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/propose-resolution")
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.proposedResolution", containsString("50% split of repair cost")))
                .andExpect(jsonPath("$.data.mediatorUserId", is(staffUser.getId().intValue())));

        // Co-owner proposes resolution -> 403 Forbidden
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/propose-resolution")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC Invariant: Staff can initiate and escalate; Staff CANNOT arbitrate RESOLVED (403); Admin CAN arbitrate RESOLVED (200)")
    void testStaffDisputeTransitions_RBAC_Enforcement() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        // 1. Staff transitions OPEN -> UNDER_REVIEW -> 200 OK
        TransitionDisputeStatusRequest reqReview = new TransitionDisputeStatusRequest(
                DisputeStatus.UNDER_REVIEW, "Staff mediation commenced", null
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/transition")
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqReview)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UNDER_REVIEW")));

        // 2. Staff transitions UNDER_REVIEW -> ESCALATED -> 200 OK
        TransitionDisputeStatusRequest reqEscalate = new TransitionDisputeStatusRequest(
                DisputeStatus.ESCALATED, "Mediation deadlock, escalating to administrator", null
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/transition")
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqEscalate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ESCALATED")));

        // 3. STAFF attempts final arbitration (ESCALATED -> RESOLVED) -> REJECTED with 403 Forbidden!
        TransitionDisputeStatusRequest reqStaffResolve = new TransitionDisputeStatusRequest(
                DisputeStatus.RESOLVED, "Staff attempting final resolution", "Arbitrated by staff"
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/transition")
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqStaffResolve)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Staff members cannot perform final binding dispute arbitration")));

        // 4. CO-OWNER attempts transition -> REJECTED with 403 Forbidden!
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/transition")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqEscalate)))
                .andExpect(status().isForbidden());

        // 5. ADMIN executes final binding arbitration (ESCALATED -> RESOLVED) -> SUCCEEDS with 200 OK!
        TransitionDisputeStatusRequest reqAdminResolve = new TransitionDisputeStatusRequest(
                DisputeStatus.RESOLVED, "Administrator final arbitration", "Reimbursed 500,000 VND from respondent fund"
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/transition")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqAdminResolve)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.resolutionSummary", is("Reimbursed 500,000 VND from respondent fund")));
    }

    @Test
    @DisplayName("07-N: POST /api/v1/disputes/{id}/arbitrate: Admin executes final binding arbitration (200 OK); Staff and Co-owner Forbidden (403); Second arbitration Conflict (409)")
    void testAdminArbitrate_RBAC_And_Conflict() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        AdminArbitrateDisputeRequest arbitrateReq = new AdminArbitrateDisputeRequest(
                "Telemetry, photos, and 3D defect coordinate mesh analysis confirms scratch occurred during respondent rental.",
                "Respondent assessed $150 repair deductible debited to syndicate shared fund. Dispute closed as RESOLVED."
        );

        // 1. Co-owner attempts arbitration -> 403 Forbidden
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/arbitrate")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(arbitrateReq)))
                .andExpect(status().isForbidden());

        // 2. Staff attempts arbitration -> 403 Forbidden
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/arbitrate")
                        .with(user(UserPrincipal.create(staffUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(arbitrateReq)))
                .andExpect(status().isForbidden());

        // 3. Unauthenticated attempts arbitration -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/arbitrate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(arbitrateReq)))
                .andExpect(status().isUnauthorized());

        // 4. Admin executes binding arbitration -> 200 OK
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/arbitrate")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(arbitrateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("RESOLVED")))
                .andExpect(jsonPath("$.data.resolutionSummary", containsString("Respondent assessed $150 repair deductible")))
                .andExpect(jsonPath("$.data.arbitratorUserId", is(adminUser.getId().intValue())))
                .andExpect(jsonPath("$.data.resolvedAt", notNullValue()));

        // 5. Subsequent arbitration on already RESOLVED dispute -> 409 Conflict
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/arbitrate")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(arbitrateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already in status 'RESOLVED'")));
    }

    @Test
    @DisplayName("07-N: POST /api/v1/disputes/{id}/arbitrate: Missing reason or resolution summary rejected with 400 Bad Request")
    void testAdminArbitrate_Validation_BlankFields() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        // Blank reason
        AdminArbitrateDisputeRequest blankReasonReq = new AdminArbitrateDisputeRequest(
                "   ", "Valid resolution summary terms"
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/arbitrate")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReasonReq)))
                .andExpect(status().isBadRequest());

        // Blank resolution summary
        AdminArbitrateDisputeRequest blankSummaryReq = new AdminArbitrateDisputeRequest(
                "Valid factual justification reason", "   "
        );
        mockMvc.perform(post("/api/v1/disputes/" + disputeId + "/arbitrate")
                        .with(user(UserPrincipal.create(adminUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankSummaryReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("07-N: GET /api/v1/disputes/{id}/arbitration-dossier: Admin evidence review (200 OK); Staff and Co-owner Forbidden (403)")
    void testAdminArbitrationDossier_RBAC_And_Review() throws Exception {
        CreateDisputeRequest request = createValidDisputePayload();
        String resJson = mockMvc.perform(post("/api/v1/disputes")
                        .with(user(UserPrincipal.create(activeOwner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long disputeId = objectMapper.readTree(resJson).path("data").path("id").asLong();

        // 1. Co-owner attempts dossier review -> 403 Forbidden
        mockMvc.perform(get("/api/v1/disputes/" + disputeId + "/arbitration-dossier")
                        .with(user(UserPrincipal.create(activeOwner))))
                .andExpect(status().isForbidden());

        // 2. Staff attempts dossier review -> 403 Forbidden
        mockMvc.perform(get("/api/v1/disputes/" + disputeId + "/arbitration-dossier")
                        .with(user(UserPrincipal.create(staffUser))))
                .andExpect(status().isForbidden());

        // 3. Admin accesses dossier -> 200 OK
        mockMvc.perform(get("/api/v1/disputes/" + disputeId + "/arbitration-dossier")
                        .with(user(UserPrincipal.create(adminUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.dispute.id", is(disputeId.intValue())))
                .andExpect(jsonPath("$.data.totalEvidences", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.evidences", is(not(empty()))))
                .andExpect(jsonPath("$.data.history", is(not(empty()))));
    }
}
