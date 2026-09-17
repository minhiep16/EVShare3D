import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useDecisionStore } from './useDecisionStore';
import {
  proposalsApi,
  type ProposalDTO,
  type ProposalResultsDTO,
  type ProposalTallyDTO,
} from '../api/proposalsApi';
import { ApiError } from '../api/apiError';

describe('Phase 09 Checkpoint 09-W: Voting Integration Subsystem', () => {
  const mockProposals: ProposalDTO[] = [
    {
      id: 101,
      groupId: 1,
      groupName: 'VinFast VF8 Syndicate Alpha',
      proposerUserId: 1,
      proposerFullName: 'Minh Hiep',
      title: 'VinFast VF8 High-Voltage Traction Battery Diagnostic & Coolant Service',
      description: 'Scheduled 40,000 km battery diagnostic inspection and coolant flush.',
      proposalType: 'ROUTINE_EXPENSE',
      status: 'ACTIVE',
      votingDeadline: '2026-09-22T18:00:00Z',
      createdAt: '2026-09-15T08:00:00Z',
      options: [
        { id: 1001, optionKey: 'APPROVE', label: 'Approve Proposal' },
        { id: 1002, optionKey: 'REJECT', label: 'Reject Proposal' },
        { id: 1003, optionKey: 'ABSTAIN', label: 'Abstain from Vote' },
      ],
    },
    {
      id: 103,
      groupId: 1,
      groupName: 'VinFast VF8 Syndicate Alpha',
      proposerUserId: 4,
      proposerFullName: 'Pham Mai',
      title: 'Charter Amendment: Increase Minimum Fund Reserve Baseline to 15,000,000 VND',
      description: 'Amend Section 4.2 to require elevated reserve baseline.',
      proposalType: 'AMENDMENT',
      status: 'ACTIVE',
      votingDeadline: '2026-09-28T18:00:00Z',
      createdAt: '2026-09-13T16:00:00Z',
      options: [
        { id: 1007, optionKey: 'APPROVE', label: 'Approve Proposal' },
        { id: 1008, optionKey: 'REJECT', label: 'Reject Proposal' },
        { id: 1009, optionKey: 'ABSTAIN', label: 'Abstain from Vote' },
      ],
    },
  ];

  const mockTallyPending: ProposalTallyDTO = {
    proposalId: 101,
    proposalTitle: 'VinFast VF8 High-Voltage Traction Battery Diagnostic & Coolant Service',
    proposalType: 'ROUTINE_EXPENSE',
    status: 'ACTIVE',
    totalGroupActiveEquity: 100.0,
    totalParticipatingEquity: 45.0,
    participationRatePercentage: 45.0,
    totalVotersCount: 2,
    quorumPercentage: 60.0,
    quorumReached: false,
    approveEquity: 45.0,
    rejectEquity: 0.0,
    abstainEquity: 0.0,
    approvePercentageOfParticipating: 100.0,
    rejectPercentageOfParticipating: 0.0,
    abstainPercentageOfParticipating: 0.0,
    approvePercentageOfTotal: 45.0,
    requiredThresholdPercentage: 50.0,
    thresholdType: 'RELATIVE_TO_PARTICIPATING',
    passed: false,
    outcomeReason: 'Quorum of 60.00% not reached. Current participation: 45.00%',
    ballots: [
      {
        id: 201,
        proposalId: 101,
        userId: 2,
        voterName: 'Tran Duc',
        voteOptionId: 1001,
        optionKey: 'APPROVE',
        optionLabel: 'Approve Proposal',
        equityWeight: 25.0,
        votedAt: '2026-09-14T09:30:00Z',
      },
      {
        id: 202,
        proposalId: 101,
        userId: 4,
        voterName: 'Pham Mai',
        voteOptionId: 1001,
        optionKey: 'APPROVE',
        optionLabel: 'Approve Proposal',
        equityWeight: 20.0,
        votedAt: '2026-09-15T14:15:00Z',
      },
    ],
  };

  const mockResultsPending: ProposalResultsDTO = {
    proposalId: 101,
    proposalTitle: 'VinFast VF8 High-Voltage Traction Battery Diagnostic & Coolant Service',
    proposalType: 'ROUTINE_EXPENSE',
    status: 'ACTIVE',
    totalEligibleEquity: 100.0,
    participatingEquity: 45.0,
    approveWeight: 45.0,
    rejectWeight: 0.0,
    abstainWeight: 0.0,
    quorumStatus: 'NOT_REACHED',
    quorumReached: false,
    quorumPercentage: 60.0,
    participationRatePercentage: 45.0,
    threshold: 50.0,
    thresholdType: 'RELATIVE_TO_PARTICIPATING',
    thresholdDescription: '> 50.00% of participating equity',
    finalDecision: 'QUORUM_NOT_MET',
    passed: false,
    decisionReason: 'Quorum of 60.00% not reached. Current participation: 45.00%',
  };

  beforeEach(() => {
    vi.restoreAllMocks();
    useDecisionStore.setState({
      proposals: mockProposals,
      selectedProposalId: 101,
      activeProposal: mockProposals[0],
      tally: mockTallyPending,
      results: mockResultsPending,
      myVote: null,
      selectedChoice: null,
      isSubmittingVote: false,
      voteError: null,
    });
  });

  describe('1. Proposal Discovery & Selection Pipeline', () => {
    it('fetches syndicate proposals from backend and maintains active selection', async () => {
      const getProposalsSpy = vi
        .spyOn(proposalsApi, 'getProposalsByGroupId')
        .mockResolvedValue(mockProposals);
      const getTallySpy = vi
        .spyOn(proposalsApi, 'getProposalTally')
        .mockResolvedValue(mockTallyPending);
      const getResultsSpy = vi
        .spyOn(proposalsApi, 'getProposalResults')
        .mockResolvedValue(mockResultsPending);
      const getMyVoteSpy = vi
        .spyOn(proposalsApi, 'getMyVote')
        .mockResolvedValue(null);

      await useDecisionStore.getState().fetchProposals(1);

      expect(getProposalsSpy).toHaveBeenCalledWith(1);
      expect(getTallySpy).toHaveBeenCalledWith(101);
      expect(getResultsSpy).toHaveBeenCalledWith(101);
      expect(getMyVoteSpy).toHaveBeenCalledWith(101);

      const state = useDecisionStore.getState();
      expect(state.proposals).toHaveLength(2);
      expect(state.selectedProposalId).toBe(101);
      expect(state.activeProposal?.title).toContain('Traction Battery');
    });

    it('synchronizes member ballot pedestal states from backend tally ballots', async () => {
      vi.spyOn(proposalsApi, 'getMyVote').mockResolvedValue(null);
      vi.spyOn(proposalsApi, 'getProposalTally').mockResolvedValue(mockTallyPending);
      vi.spyOn(proposalsApi, 'getProposalResults').mockResolvedValue(mockResultsPending);

      await useDecisionStore.getState().selectProposal(101);

      const members = useDecisionStore.getState().members;
      const tranDuc = members.find((m) => m.userId === 2);
      const phamMai = members.find((m) => m.userId === 4);
      const minhHiep = members.find((m) => m.userId === 1);

      expect(tranDuc?.hasVoted).toBe(true);
      expect(tranDuc?.voteChoice).toBe('APPROVE');
      expect(phamMai?.hasVoted).toBe(true);
      expect(phamMai?.voteChoice).toBe('APPROVE');
      expect(minhHiep?.hasVoted).toBe(false);
    });
  });

  describe('2. Strict Non-Override Invariant (Frontend Visualizes Backend Results)', () => {
    it('never overrides backend decision when quorum is pending', async () => {
      vi.spyOn(proposalsApi, 'getMyVote').mockResolvedValue(null);
      vi.spyOn(proposalsApi, 'getProposalTally').mockResolvedValue(mockTallyPending);
      vi.spyOn(proposalsApi, 'getProposalResults').mockResolvedValue(mockResultsPending);

      await useDecisionStore.getState().selectProposal(101);

      const { results, tally } = useDecisionStore.getState();
      expect(results?.finalDecision).toBe('QUORUM_NOT_MET');
      expect(results?.passed).toBe(false);
      expect(results?.quorumReached).toBe(false);
      expect(results?.decisionReason).toBe('Quorum of 60.00% not reached. Current participation: 45.00%');
      expect(tally?.passed).toBe(false);
    });

    it('never overrides supermajority failure to passed for AMENDMENT proposals', async () => {
      // Co-owners vote 65% APPROVE, 15% REJECT = 80% participation (quorum satisfied).
      // But AMENDMENT requires >= 75.00% of total equity to pass.
      const mockSupermajorityFailTally: ProposalTallyDTO = {
        proposalId: 103,
        proposalTitle: 'Charter Amendment',
        proposalType: 'AMENDMENT',
        status: 'ACTIVE',
        totalGroupActiveEquity: 100.0,
        totalParticipatingEquity: 80.0,
        participationRatePercentage: 80.0,
        totalVotersCount: 3,
        quorumPercentage: 60.0,
        quorumReached: true,
        approveEquity: 65.0,
        rejectEquity: 15.0,
        abstainEquity: 0.0,
        approvePercentageOfParticipating: 81.25,
        rejectPercentageOfParticipating: 18.75,
        abstainPercentageOfParticipating: 0.0,
        approvePercentageOfTotal: 65.0,
        requiredThresholdPercentage: 75.0,
        thresholdType: 'RELATIVE_TO_TOTAL',
        passed: false, // Backend evaluated: 65.00% < 75.00% supermajority
        outcomeReason: 'Rejected: Failed 75.00% supermajority threshold (achieved 65.00%)',
        ballots: [],
      };

      const mockSupermajorityFailResults: ProposalResultsDTO = {
        proposalId: 103,
        proposalTitle: 'Charter Amendment',
        proposalType: 'AMENDMENT',
        status: 'ACTIVE',
        totalEligibleEquity: 100.0,
        participatingEquity: 80.0,
        approveWeight: 65.0,
        rejectWeight: 15.0,
        abstainWeight: 0.0,
        quorumStatus: 'REACHED',
        quorumReached: true,
        quorumPercentage: 60.0,
        participationRatePercentage: 80.0,
        threshold: 75.0,
        thresholdType: 'RELATIVE_TO_TOTAL',
        thresholdDescription: '>= 75.00% of total eligible equity',
        finalDecision: 'REJECTED',
        passed: false,
        decisionReason: 'Rejected: Failed 75.00% supermajority threshold (achieved 65.00%)',
      };

      vi.spyOn(proposalsApi, 'getMyVote').mockResolvedValue(null);
      vi.spyOn(proposalsApi, 'getProposalTally').mockResolvedValue(mockSupermajorityFailTally);
      vi.spyOn(proposalsApi, 'getProposalResults').mockResolvedValue(mockSupermajorityFailResults);

      await useDecisionStore.getState().selectProposal(103);

      const { results } = useDecisionStore.getState();
      // Even though 81.25% of participants voted YES and quorum is satisfied,
      // frontend visualizes backend REJECTED verdict and MUST NOT override!
      expect(results?.finalDecision).toBe('REJECTED');
      expect(results?.passed).toBe(false);
      expect(results?.threshold).toBe(75.0);
      expect(results?.thresholdDescription).toBe('>= 75.00% of total eligible equity');
      expect(results?.decisionReason).toContain('Failed 75.00% supermajority');
    });
  });

  describe('3. Ballot Casting & Synchronous Decision Propagation', () => {
    it('dispatches castVote to backend and updates authoritative results on success', async () => {
      const castVoteSpy = vi.spyOn(proposalsApi, 'castVote').mockResolvedValue({
        id: 301,
        proposalId: 101,
        userId: 1,
        voterName: 'Minh Hiep',
        voteOptionId: 1001,
        optionKey: 'APPROVE',
        optionLabel: 'Approve Proposal',
        equityWeight: 35.0,
        votedAt: '2026-09-16T14:30:00Z',
      });

      const mockTallyPassed: ProposalTallyDTO = {
        ...mockTallyPending,
        totalParticipatingEquity: 80.0,
        participationRatePercentage: 80.0,
        totalVotersCount: 3,
        quorumReached: true,
        approveEquity: 80.0,
        passed: true,
        outcomeReason: 'Approved with 100.00% of participating equity (>50.00%)',
      };

      const mockResultsPassed: ProposalResultsDTO = {
        ...mockResultsPending,
        participatingEquity: 80.0,
        approveWeight: 80.0,
        quorumStatus: 'REACHED',
        quorumReached: true,
        participationRatePercentage: 80.0,
        finalDecision: 'PASSED',
        passed: true,
        decisionReason: 'Approved with 100.00% of participating equity (>50.00%)',
      };

      vi.spyOn(proposalsApi, 'getProposalTally').mockResolvedValue(mockTallyPassed);
      vi.spyOn(proposalsApi, 'getProposalResults').mockResolvedValue(mockResultsPassed);
      vi.spyOn(proposalsApi, 'getMyVote').mockResolvedValue({
        id: 301,
        proposalId: 101,
        userId: 1,
        voterName: 'Minh Hiep',
        voteOptionId: 1001,
        optionKey: 'APPROVE',
        optionLabel: 'Approve Proposal',
        equityWeight: 35.0,
        votedAt: '2026-09-16T14:30:00Z',
      });

      const success = await useDecisionStore.getState().castVote('APPROVE');
      expect(success).toBe(true);
      expect(castVoteSpy).toHaveBeenCalledWith(101, { optionKey: 'APPROVE' });

      const state = useDecisionStore.getState();
      expect(state.myVote?.optionKey).toBe('APPROVE');
      expect(state.results?.finalDecision).toBe('PASSED');
      expect(state.results?.passed).toBe(true);
      expect(state.results?.quorumReached).toBe(true);
      expect(state.results?.decisionReason).toContain('Approved with 100.00%');
    });

    it('rejects duplicate ballot when backend returns HTTP 409 Conflict without overriding decision', async () => {
      // Simulate Spring Boot 409 Conflict response
      const conflictError = new ApiError({
        message: 'User 1 has already cast a ballot on proposal 101',
        status: 409,
        code: 'CONFLICT',
        rawError: {
          response: {
            status: 409,
            data: {
              message: 'User 1 has already cast a ballot on proposal 101',
              status: 409,
            },
          },
        },
      });

      vi.spyOn(proposalsApi, 'castVote').mockRejectedValue(conflictError);

      const success = await useDecisionStore.getState().castVote('APPROVE');
      expect(success).toBe(false);

      const state = useDecisionStore.getState();
      expect(state.voteError).toBe('User 1 has already cast a ballot on proposal 101');
      expect(state.isSubmittingVote).toBe(false);

      // Decision state remains unmodified
      expect(state.results?.finalDecision).toBe('QUORUM_NOT_MET');
      expect(state.results?.passed).toBe(false);
    });

    it('handles voting deadline expiration error from backend (HTTP 400)', async () => {
      const expiredError = new ApiError({
        message: 'Voting deadline for proposal 101 has expired',
        status: 400,
        code: 'BAD_REQUEST',
        rawError: {
          response: {
            status: 400,
            data: {
              message: 'Voting deadline for proposal 101 has expired',
              status: 400,
            },
          },
        },
      });

      vi.spyOn(proposalsApi, 'castVote').mockRejectedValue(expiredError);

      const success = await useDecisionStore.getState().castVote('REJECT');
      expect(success).toBe(false);

      const state = useDecisionStore.getState();
      expect(state.voteError).toContain('Voting deadline');
      expect(state.isSubmittingVote).toBe(false);
    });
  });

  describe('4. Quorum Monitor & Threshold Visualization Bindings', () => {
    it('provides accurate quorum threshold and rate metrics to QuorumLiquidColumn3D', () => {
      const state = useDecisionStore.getState();
      expect(state.results?.quorumPercentage).toBe(60.0);
      expect(state.results?.participationRatePercentage).toBe(45.0);
      expect(state.results?.quorumReached).toBe(false);
    });

    it('provides decision thresholds and justification to DecisionResultStela3D', () => {
      const state = useDecisionStore.getState();
      expect(state.results?.threshold).toBe(50.0);
      expect(state.results?.thresholdType).toBe('RELATIVE_TO_PARTICIPATING');
      expect(state.results?.thresholdDescription).toBe('> 50.00% of participating equity');
      expect(state.results?.finalDecision).toBe('QUORUM_NOT_MET');
    });
  });
});
