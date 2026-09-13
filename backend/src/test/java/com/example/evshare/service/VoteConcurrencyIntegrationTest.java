package com.example.evshare.service;

import com.example.evshare.dto.request.CastVoteRequest;
import com.example.evshare.dto.response.VoteResponse;
import com.example.evshare.entity.*;
import com.example.evshare.entity.enums.*;
import com.example.evshare.exception.BusinessException;
import com.example.evshare.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Checkpoint 07-F — Concurrent Duplicate Vote Prevention Integration Tests")
class VoteConcurrencyIntegrationTest {

    private static final AtomicLong PHONE_SEQ = new AtomicLong(System.currentTimeMillis() % 80000000L + 10000000L);

    @Autowired private VotingService votingService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private OwnershipGroupRepository ownershipGroupRepository;
    @Autowired private OwnershipShareRepository ownershipShareRepository;
    @Autowired private ProposalRepository proposalRepository;
    @Autowired private VoteOptionRepository voteOptionRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private User coOwner;
    private Vehicle vehicle;
    private OwnershipGroup group;
    private OwnershipShare share;
    private Proposal proposal;

    @BeforeEach
    void setUp() {
        Role roleCoOwner = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        String uid = UUID.randomUUID().toString().substring(0, 8);
        coOwner = new User();
        coOwner.setEmail("voter.concur." + uid + "@evshare.io");
        coOwner.setFullName("Concurrent Voter " + uid);
        coOwner.setPasswordHash("$2a$12$dummyPasswordHashForSessionTesting12345");
        coOwner.setPhoneNumber("09" + PHONE_SEQ.incrementAndGet());
        coOwner.setIsActive(true);
        coOwner.setRoles(Set.of(roleCoOwner));
        coOwner = userRepository.saveAndFlush(coOwner);

        vehicle = new Vehicle();
        vehicle.setVin("VIN" + uid.toUpperCase() + "112233");
        vehicle.setLicensePlate("51K-" + uid.toUpperCase());
        vehicle.setModelName("VinFast VF8 Plus");
        vehicle.setManufacturer("VinFast");
        vehicle.setModel3dAssetPath("models/vehicles/vf8.glb");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle = vehicleRepository.saveAndFlush(vehicle);

        group = new OwnershipGroup();
        group.setGroupName("Concurrency Voting Group " + uid);
        group.setVehicle(vehicle);
        group.setFormationDate(LocalDate.now());
        group.setIsActive(true);
        group = ownershipGroupRepository.saveAndFlush(group);

        share = new OwnershipShare();
        share.setGroup(group);
        share.setUser(coOwner);
        share.setPercentage(new BigDecimal("50.00"));
        share.setShareCertificateNumber("CERT-VOTE-" + uid);
        share.setIsActive(true);
        share = ownershipShareRepository.saveAndFlush(share);

        proposal = new Proposal();
        proposal.setGroup(group);
        proposal.setProposerUser(coOwner);
        proposal.setTitle("Concurrent Race Prevention Proposal " + uid);
        proposal.setDescription("Testing thread safety of ballot casting");
        proposal.setProposalType(ProposalType.ROUTINE_EXPENSE);
        proposal.setStatus(ProposalStatus.ACTIVE);
        proposal.setVotingDeadline(Instant.now().plus(72, ChronoUnit.HOURS));
        proposal = proposalRepository.saveAndFlush(proposal);

        VoteOption opt1 = new VoteOption(null, proposal, VoteOptionKey.APPROVE, "Approve");
        VoteOption opt2 = new VoteOption(null, proposal, VoteOptionKey.REJECT, "Reject");
        VoteOption opt3 = new VoteOption(null, proposal, VoteOptionKey.ABSTAIN, "Abstain");
        voteOptionRepository.saveAllAndFlush(List.of(opt1, opt2, opt3));
    }

    @AfterEach
    void tearDown() {
        try {
            if (proposal != null && proposal.getId() != null) {
                auditLogRepository.deleteAll(auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Proposal", proposal.getId()));
                auditLogRepository.deleteAll(auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("Vote", proposal.getId()));
                voteRepository.deleteAll(voteRepository.findByProposalId(proposal.getId()));
                voteOptionRepository.deleteAll(voteOptionRepository.findByProposalId(proposal.getId()));
                proposalRepository.deleteById(proposal.getId());
            }
            if (share != null && share.getId() != null) {
                ownershipShareRepository.deleteById(share.getId());
            }
            if (group != null && group.getId() != null) {
                ownershipGroupRepository.deleteById(group.getId());
            }
            if (vehicle != null && vehicle.getId() != null) {
                vehicleRepository.deleteById(vehicle.getId());
            }
            if (coOwner != null && coOwner.getId() != null) {
                userRepository.deleteById(coOwner.getId());
            }
        } catch (Exception e) {
            // Clean-up safeguard
        }
    }

    @Test
    @DisplayName("Concurrent Duplicate Prevention: 10 parallel threads attempt to cast vote simultaneously; exactly 1 succeeds")
    void testConcurrentVoteCasting_ExactlyOneBallotPersisted() throws Exception {
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CastVoteRequest request = new CastVoteRequest(VoteOptionKey.APPROVE);
                    VoteResponse response = votingService.castVote(proposal.getId(), request, coOwner.getId());
                    if (response != null && response.getId() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (BusinessException be) {
                    if (be.getStatus() == HttpStatus.CONFLICT) {
                        conflictCount.incrementAndGet();
                    } else {
                        unexpectedErrors.add(be);
                    }
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 10 concurrent voting threads must complete within timeout");
        assertTrue(unexpectedErrors.isEmpty(), "No unexpected exceptions during concurrent voting: " + unexpectedErrors);
        assertEquals(1, successCount.get(), "Strictly one ballot must be accepted among 10 concurrent threads");
        assertEquals(numThreads - 1, conflictCount.get(), "All 9 losing concurrent threads must be rejected with HTTP 409 Conflict");

        // Verify database persistence guarantees
        List<Vote> persistedVotes = voteRepository.findByProposalId(proposal.getId());
        assertEquals(1, persistedVotes.size(), "Database must strictly contain exactly 1 ballot record for this proposal");
        assertEquals(coOwner.getId(), persistedVotes.get(0).getUser().getId());
        assertEquals(new BigDecimal("50.00"), persistedVotes.get(0).getEquityWeight());
    }
}
