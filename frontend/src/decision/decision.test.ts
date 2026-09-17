import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useDecisionStore } from './useDecisionStore';
import { DECISION_LAYOUT } from './decisionLayout';
import { proposalsApi } from '../api/proposalsApi';

describe('Decision Chamber Subsystem (09-I)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useDecisionStore.setState({
      selectedProposalId: 101,
      selectedChoice: null,
      myVote: null,
      isSubmittingVote: false,
      voteError: null,
      activeTab: 'OVERVIEW',
      activeStation: 'POD_CLUSTER',
    });
  });

  describe('1. Spatial Layout & Camera Presets', () => {
    it('defines correct sector center and station positions', () => {
      expect(DECISION_LAYOUT.sectorCenter).toEqual([40, 0, -40]);
      expect(DECISION_LAYOUT.votingTerminalPosition).toEqual([0, 0, 1.2]);
      expect(DECISION_LAYOUT.proposalPodClusterPosition).toEqual([0, 0, -3.2]);
      expect(DECISION_LAYOUT.equityPillarsPosition).toEqual([-4.5, 0, -0.5]);
      expect(DECISION_LAYOUT.quorumColumnPosition).toEqual([4.5, 0, -0.5]);
      expect(DECISION_LAYOUT.resultStelaPosition).toEqual([0, 0, -6.5]);
    });

    it('provides all 6 camera presets for parliamentary stations', () => {
      const presets = DECISION_LAYOUT.cameraPresets;
      expect(presets.ARENA_OVERVIEW).toBeDefined();
      expect(presets.TERMINAL_FOCUS).toBeDefined();
      expect(presets.PROPOSAL_PODS_FOCUS).toBeDefined();
      expect(presets.EQUITY_FOCUS).toBeDefined();
      expect(presets.QUORUM_RESULTS_FOCUS).toBeDefined();
      expect(presets.STELA_FOCUS).toBeDefined();

      expect(presets.ARENA_OVERVIEW.position).toHaveLength(3);
      expect(presets.ARENA_OVERVIEW.target).toHaveLength(3);
    });

    it('contains parliament indigo theme tokens', () => {
      const colors = DECISION_LAYOUT.colors;
      expect(colors.arenaDark).toBe('#080a14');
      expect(colors.indigoPrimary).toBe('#4f46e5');
      expect(colors.violetNeon).toBe('#a855f7');
      expect(colors.cyanQuorum).toBe('#06b6d4');
      expect(colors.goldThreshold).toBe('#fbbf24');
      expect(colors.approveEmerald).toBe('#10b981');
      expect(colors.rejectRuby).toBe('#ef4444');
      expect(colors.abstainAmber).toBe('#f59e0b');
    });
  });

  describe('2. Member Equity Distribution (BR-VOT-01)', () => {
    it('allocates 100.00% total equity across the 4 syndicate co-owners', () => {
      const members = useDecisionStore.getState().members;
      expect(members).toHaveLength(4);

      const totalEquity = members.reduce((sum, m) => sum + m.equityPercentage, 0);
      expect(totalEquity).toBeCloseTo(100.0, 2);

      const minhHiep = members.find((m) => m.userId === 1);
      expect(minhHiep?.memberName).toBe('Minh Hiep');
      expect(minhHiep?.equityPercentage).toBe(35.0);

      const tranDuc = members.find((m) => m.userId === 2);
      expect(tranDuc?.memberName).toBe('Tran Duc');
      expect(tranDuc?.equityPercentage).toBe(25.0);

      const leHoang = members.find((m) => m.userId === 3);
      expect(leHoang?.memberName).toBe('Le Hoang');
      expect(leHoang?.equityPercentage).toBe(20.0);

      const phamMai = members.find((m) => m.userId === 4);
      expect(phamMai?.memberName).toBe('Pham Mai');
      expect(phamMai?.equityPercentage).toBe(20.0);
    });
  });

  describe('3. Proposal Deliberation & Selection', () => {
    it('initializes with active proposals across categories', () => {
      const proposals = useDecisionStore.getState().proposals;
      expect(proposals.length).toBeGreaterThanOrEqual(3);

      const types = proposals.map((p) => p.proposalType);
      expect(types).toContain('ROUTINE_EXPENSE');
      expect(types).toContain('ASSET_UPGRADE');
      expect(types).toContain('AMENDMENT');
    });

    it('switches active proposal and resets transient vote state', async () => {
      const store = useDecisionStore.getState();
      await store.selectProposal(102);

      const updated = useDecisionStore.getState();
      expect(updated.selectedProposalId).toBe(102);
      expect(updated.activeProposal?.id).toBe(102);
      expect(updated.activeProposal?.title).toContain('Level 2 Home Dual-Port Wallbox');
      expect(updated.selectedChoice).toBeNull();
    });
  });

  describe('4. Authoritative Voting & Quorum Evaluation', () => {
    it('evaluates quorum as pending (<60.00%) before voter casts ballot', () => {
      const { tally, results } = useDecisionStore.getState();
      expect(tally).toBeDefined();
      expect(tally?.quorumPercentage).toBe(60.0);
      // Tran Duc (25%) + Pham Mai (20%) = 45.0%
      expect(tally?.totalParticipatingEquity).toBe(45.0);
      expect(tally?.quorumReached).toBe(false);
      expect(results?.quorumStatus).toBe('NOT_REACHED');
      expect(results?.passed).toBe(false);
      expect(results?.finalDecision).toBe('QUORUM_NOT_MET');
    });

    it('records user ballot and satisfies 60.00% quorum when Minh Hiep casts APPROVE', async () => {
      vi.spyOn(proposalsApi, 'castVote').mockResolvedValue({
        id: 203,
        proposalId: 101,
        userId: 1,
        voterName: 'Minh Hiep',
        voteOptionId: 1001,
        optionKey: 'APPROVE',
        optionLabel: 'APPROVE PROPOSAL',
        equityWeight: 35.0,
        votedAt: new Date().toISOString(),
      });
      vi.spyOn(proposalsApi, 'getProposalTally').mockRejectedValue(new Error('offline fallback'));
      vi.spyOn(proposalsApi, 'getProposalResults').mockRejectedValue(new Error('offline fallback'));
      vi.spyOn(proposalsApi, 'getMyVote').mockRejectedValue(new Error('offline fallback'));

      const store = useDecisionStore.getState();
      store.setSelectedChoice('APPROVE');
      expect(useDecisionStore.getState().selectedChoice).toBe('APPROVE');

      const success = await store.castVote('APPROVE');
      expect(success).toBe(true);

      const updated = useDecisionStore.getState();
      expect(updated.myVote).toBeDefined();
      expect(updated.myVote?.optionKey).toBe('APPROVE');
      expect(updated.myVote?.equityWeight).toBe(35.0);

      // Now 45% + 35% = 80.0% participating equity
      expect(updated.tally?.totalParticipatingEquity).toBe(80.0);
      expect(updated.tally?.quorumReached).toBe(true);
      expect(updated.tally?.passed).toBe(true);

      // Official results from backend
      expect(updated.results?.quorumReached).toBe(true);
      expect(updated.results?.finalDecision).toBe('PASSED');
      expect(updated.results?.passed).toBe(true);
      expect(updated.results?.decisionReason).toContain('Quorum met');
    });

    it('correctly records REJECT choice without breaking quorum mathematics', async () => {
      vi.spyOn(proposalsApi, 'castVote').mockResolvedValue({
        id: 203,
        proposalId: 101,
        userId: 1,
        voterName: 'Minh Hiep',
        voteOptionId: 1002,
        optionKey: 'REJECT',
        optionLabel: 'REJECT PROPOSAL',
        equityWeight: 35.0,
        votedAt: new Date().toISOString(),
      });
      vi.spyOn(proposalsApi, 'getProposalTally').mockRejectedValue(new Error('offline fallback'));
      vi.spyOn(proposalsApi, 'getProposalResults').mockRejectedValue(new Error('offline fallback'));
      vi.spyOn(proposalsApi, 'getMyVote').mockRejectedValue(new Error('offline fallback'));

      const store = useDecisionStore.getState();
      const success = await store.castVote('REJECT');
      expect(success).toBe(true);

      const updated = useDecisionStore.getState();
      expect(updated.myVote?.optionKey).toBe('REJECT');
      expect(updated.tally?.rejectEquity).toBe(35.0);
      expect(updated.tally?.approveEquity).toBe(45.0);
      expect(updated.tally?.totalParticipatingEquity).toBe(80.0);
      expect(updated.tally?.quorumReached).toBe(true);
    });
  });

  describe('5. Backend API Service Contract', () => {
    it('exposes all required API methods in proposalsApi', () => {
      expect(typeof proposalsApi.getProposalsByGroupId).toBe('function');
      expect(typeof proposalsApi.getProposalById).toBe('function');
      expect(typeof proposalsApi.castVote).toBe('function');
      expect(typeof proposalsApi.getMyVote).toBe('function');
      expect(typeof proposalsApi.getProposalTally).toBe('function');
      expect(typeof proposalsApi.getProposalResults).toBe('function');
    });
  });
});
