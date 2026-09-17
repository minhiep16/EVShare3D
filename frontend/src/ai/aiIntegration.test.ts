import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useAIStore } from './useAIStore';
import { aiApi } from '../api/aiApi';
import { analyticsApi } from '../api/analyticsApi';
import { AISafetyEnforcer } from './aiSafety';
import type { AISafetyActionType } from './aiTypes';

// Mock both aiApi and analyticsApi
vi.mock('../api/aiApi', () => ({
  aiApi: {
    checkAIStatus: vi.fn(),
    getAIRecommendations: vi.fn(),
    queryAI: vi.fn(),
  },
}));

vi.mock('../api/analyticsApi', () => ({
  analyticsApi: {
    getGroupFairUsage: vi.fn(),
    getMyFairUsageScore: vi.fn(),
  },
}));

describe('09-Y AI Integration & Safety Boundary Test Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAIStore.setState({
      modelStatus: 'NOT_AVAILABLE',
      modelName: 'EVShare Syndicate Mobility Advisory Engine',
      isProductionAI: false,
      advisoryOnly: true,
      disclosureNotice: 'STATUS: NOT_AVAILABLE (AI API UNCONFIGURED) — ADVISORY HEURISTICS ACTIVE',
      activeCategory: 'RECOMMENDATION',
      selectedRecommendationId: 'REC_OFF_PEAK_CHARGING',
      selectedNodeId: 'NODE_MOBILITY',
      activePrompt: null,
      isAnalyzing: false,
      queryResult: null,
      lastBlockedAttempt: null,
      blockedAttemptsHistory: [],
    });
  });

  describe('1. AI Availability Probing & NOT_AVAILABLE Reporting', () => {
    it('reports NOT_AVAILABLE when AI API is unconfigured or unreachable', async () => {
      vi.mocked(aiApi.checkAIStatus).mockResolvedValue({
        available: false,
        modelStatus: 'NOT_AVAILABLE',
        message: 'AI Service unconfigured on backend',
      });

      await useAIStore.getState().checkAIStatus();

      const state = useAIStore.getState();
      expect(state.modelStatus).toBe('NOT_AVAILABLE');
      expect(state.isProductionAI).toBe(false);
      expect(state.advisoryOnly).toBe(true);
      expect(state.disclosureNotice).toContain('NOT_AVAILABLE');
    });

    it('reports NOT_AVAILABLE when network probe throws an error', async () => {
      vi.mocked(aiApi.checkAIStatus).mockRejectedValue(new Error('Network unreachable (503 Service Unavailable)'));

      await useAIStore.getState().checkAIStatus();

      const state = useAIStore.getState();
      expect(state.modelStatus).toBe('NOT_AVAILABLE');
      expect(state.isProductionAI).toBe(false);
      expect(state.disclosureNotice).toContain('NOT_AVAILABLE');
    });

    it('transitions to PRODUCTION_ONLINE when an actual AI API is available', async () => {
      vi.mocked(aiApi.checkAIStatus).mockResolvedValue({
        available: true,
        modelStatus: 'PRODUCTION_ONLINE',
        modelName: 'EVShare Syndicate Neural Assistant v4.2',
        provider: 'Gemini Enterprise Vertex',
      });

      await useAIStore.getState().checkAIStatus();

      const state = useAIStore.getState();
      expect(state.modelStatus).toBe('PRODUCTION_ONLINE');
      expect(state.isProductionAI).toBe(true);
      expect(state.advisoryOnly).toBe(true);
      expect(state.modelName).toBe('EVShare Syndicate Neural Assistant v4.2');
      expect(state.disclosureNotice).toContain('ONLINE');
      expect(state.disclosureNotice).toContain('ADVISORY ONLY');
    });
  });

  describe('2. Representation as Recommendation & Insight Only', () => {
    it('ensures all generated/fetched recommendations are strictly advisory', async () => {
      vi.mocked(aiApi.checkAIStatus).mockResolvedValue({
        available: true,
        modelStatus: 'PRODUCTION_ONLINE',
        modelName: 'Production Neural Engine',
      });

      vi.mocked(aiApi.getAIRecommendations).mockResolvedValue([
        {
          id: 'LIVE_REC_1',
          title: 'Battery Cell Balancing Recommendation',
          category: 'RECOMMENDATION',
          severity: 'INFO',
          confidenceScore: 97.5,
          description: 'Recommend slow AC charging to balance cell clusters.',
          impactMetric: '+1.2% SOH longevity',
          suggestedAction: 'Schedule AC session',
          isAdvisoryOnly: false, // Attempt to mark as non-advisory
          isMock: false,
          timestamp: '2026-09-16T12:00:00Z',
        },
      ]);

      await useAIStore.getState().fetchAIData(1);

      const state = useAIStore.getState();
      const rec = state.recommendations.find((r) => r.id === 'LIVE_REC_1');
      expect(rec).toBeDefined();
      // Store must enforce advisory only flag
      expect(rec?.isAdvisoryOnly).toBe(true);
      expect(['RECOMMENDATION', 'INSIGHT']).toContain(rec?.category);
    });

    it('formats interactive query responses as recommendations or insights', async () => {
      // 1. Fallback heuristic synthesis
      await useAIStore.getState().runInteractiveQuery('ANALYZE SYNDICATE FAIRNESS');
      let result = useAIStore.getState().queryResult;
      expect(result).toMatch(/^\[(RECOMMENDATION|INSIGHT)\]/);

      await useAIStore.getState().runInteractiveQuery('OPTIMIZE CHARGING TARIFFS');
      result = useAIStore.getState().queryResult;
      expect(result).toMatch(/^\[(RECOMMENDATION|INSIGHT)\]/);

      // 2. Connected AI API synthesis
      useAIStore.setState({ modelStatus: 'PRODUCTION_ONLINE' });
      vi.mocked(aiApi.queryAI).mockResolvedValue({
        category: 'INSIGHT',
        resultText: 'Peak consumption variance is 14% lower than syndicated benchmark.',
        confidenceScore: 95.8,
        isAdvisoryOnly: true,
        generatedAt: '2026-09-16T12:00:00Z',
      });

      await useAIStore.getState().runInteractiveQuery('FLEET UTILIZATION');
      result = useAIStore.getState().queryResult;
      expect(result).toContain('[INSIGHT]');
      expect(result).toContain('95.8%');
    });
  });

  describe('3. Hardcoded Non-Bypassable Safety Boundaries', () => {
    const requiredBypassVectors: Array<{ action: AISafetyActionType; name: string }> = [
      { action: 'BYPASS_AUTHENTICATION', name: 'Authentication' },
      { action: 'BYPASS_RBAC', name: 'Authorization / RBAC' },
      { action: 'ALTER_OWNERSHIP', name: 'Ownership Rules' },
      { action: 'AUTHORIZE_PAYMENT', name: 'Payment Controls' },
      { action: 'APPROVE_CONTRACT', name: 'Contract Lifecycle' },
      { action: 'OVERRIDE_VOTING_RULES', name: 'Voting Rules' },
      { action: 'IRREVERSIBLE_FINANCIAL_ACTION', name: 'Irreversible Financial Operations' },
    ];

    it.each(requiredBypassVectors)(
      'hard-blocks autonomous AI execution for $name ($action)',
      ({ action }) => {
        // Enforcer invariant: must unconditionally return false
        expect(AISafetyEnforcer.canAIExecuteAutonomously(action)).toBe(false);
      }
    );

    it.each(requiredBypassVectors)(
      'intercepts and records immutable audit trail when attempting to bypass $name ($action)',
      ({ action }) => {
        const store = useAIStore.getState();
        store.triggerSafetyAttempt(action);

        const updated = useAIStore.getState();
        expect(updated.lastBlockedAttempt).not.toBeNull();
        expect(updated.lastBlockedAttempt?.attemptedAction).toBe(action);
        expect(updated.lastBlockedAttempt?.blockedReason).toContain('VIOLATION');
        expect(updated.lastBlockedAttempt?.requiredAuthority).toBeDefined();
        expect(updated.blockedAttemptsHistory.length).toBeGreaterThan(0);
      }
    );

    it('blocks AI from bypassing user authentication (JWT credentials)', () => {
      useAIStore.getState().triggerSafetyAttempt('BYPASS_AUTHENTICATION');
      const record = useAIStore.getState().lastBlockedAttempt;

      expect(record?.attemptedAction).toBe('BYPASS_AUTHENTICATION');
      expect(record?.blockedReason).toContain('cannot forge, bypass, or substitute cryptographic user credentials');
      expect(record?.requiredAuthority).toBe('CRYPTOGRAPHIC_USER_CREDENTIALS_AND_JWT');
    });

    it('blocks AI from overriding syndicate voting rules or quorum', () => {
      useAIStore.getState().triggerSafetyAttempt('OVERRIDE_VOTING_RULES');
      const record = useAIStore.getState().lastBlockedAttempt;

      expect(record?.attemptedAction).toBe('OVERRIDE_VOTING_RULES');
      expect(record?.blockedReason).toContain('cannot cast ballots, bypass the 60.00% quorum requirement');
      expect(record?.requiredAuthority).toBe('DEMOCRATIC_CO_OWNER_BALLOT_QUORUM');
    });

    it('blocks AI from bypassing payment controls or executing fund withdrawals', () => {
      useAIStore.getState().triggerSafetyAttempt('AUTHORIZE_PAYMENT');
      const record = useAIStore.getState().lastBlockedAttempt;

      expect(record?.attemptedAction).toBe('AUTHORIZE_PAYMENT');
      expect(record?.blockedReason).toContain('require explicit 2FA biometric authorization');
      expect(record?.requiredAuthority).toBe('HUMAN_CO_OWNER_BIOMETRIC_SIGNATURE');
    });

    it('blocks AI from altering ownership shares or member cap tables', () => {
      useAIStore.getState().triggerSafetyAttempt('ALTER_OWNERSHIP');
      const record = useAIStore.getState().lastBlockedAttempt;

      expect(record?.attemptedAction).toBe('ALTER_OWNERSHIP');
      expect(record?.blockedReason).toContain('cannot modify cap tables or syndicate equity allocations');
      expect(record?.requiredAuthority).toContain('75% SUPERMAJORITY');
    });

    it('blocks AI from approving or ratifying legal contracts', () => {
      useAIStore.getState().triggerSafetyAttempt('APPROVE_CONTRACT');
      const record = useAIStore.getState().lastBlockedAttempt;

      expect(record?.attemptedAction).toBe('APPROVE_CONTRACT');
      expect(record?.blockedReason).toContain('verifiable personal digital signatures');
      expect(record?.requiredAuthority).toBe('AUTHENTICATED_CO_OWNER_LEGAL_SIGNATURE');
    });
  });

  describe('4. Deterministic Analytics Fallback', () => {
    it('enriches data nodes using real backend analytics even when generative AI is NOT_AVAILABLE', async () => {
      vi.mocked(aiApi.checkAIStatus).mockResolvedValue({
        available: false,
        modelStatus: 'NOT_AVAILABLE',
      });

      vi.mocked(analyticsApi.getGroupFairUsage).mockResolvedValue({
        groupId: 1,
        groupName: 'Tesla Syndicate',
        totalGroupUsageHours: 198,
        windowDays: 30,
        windowStartDate: '2026-08-17T00:00:00Z',
        windowEndDate: '2026-09-16T00:00:00Z',
        giniCoefficient: 0.12,
        isEquityBalanced: true,
        memberMetrics: [
          {
            userId: 1,
            userName: 'Minh Hiep',
            equityPercentage: 35.0,
            actualUsageHours: 70,
            fairShareHours: 69.3,
            fairnessRatio: 1.01,
            imbalanceLevel: 'BALANCED',
            bookingPriorityScore: 100,
          },
        ],
      });

      await useAIStore.getState().fetchAIData(1);

      const state = useAIStore.getState();
      expect(state.modelStatus).toBe('NOT_AVAILABLE');
      const fairnessNode = state.dataNodes.find((n) => n.id === 'NODE_FAIRNESS');
      expect(fairnessNode).toBeDefined();

      const usageMetric = fairnessNode?.metrics.find((m) => m.label === 'Total Group Usage');
      expect(usageMetric?.value).toBe('198 Hours');
    });
  });
});
