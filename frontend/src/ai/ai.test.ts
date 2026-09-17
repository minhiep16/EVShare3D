import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useAIStore } from './useAIStore';
import { AI_LAYOUT } from './aiLayout';
import { AISafetyEnforcer, AI_SAFETY_RULES } from './aiSafety';
import type { AISafetyActionType } from './aiTypes';

describe('AI Intelligence Center Subsystem (09-J)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAIStore.setState({
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

  describe('1. Spatial Layout & Camera Presets', () => {
    it('defines correct sector center and station coordinates', () => {
      expect(AI_LAYOUT.sectorCenter).toEqual([40, 0, 0]);
      expect(AI_LAYOUT.aiCorePosition).toEqual([0, 2.5, 0]);
      expect(AI_LAYOUT.interactionTerminalPosition).toEqual([0, 0, 3.2]);
      expect(AI_LAYOUT.recommendationHoloPosition).toEqual([0, 2.2, -3.2]);
      expect(AI_LAYOUT.resultVisualizationPosition).toEqual([0, 0.2, -5.4]);
      expect(AI_LAYOUT.dataNodePositions.NODE_MOBILITY).toEqual([-3.6, 2.2, -0.8]);
      expect(AI_LAYOUT.dataNodePositions.NODE_FAIRNESS).toEqual([3.6, 2.2, -0.8]);
    });

    it('provides all 6 camera presets for neural stations', () => {
      const presets = AI_LAYOUT.cameraPresets;
      expect(presets.NEXUS_OVERVIEW).toBeDefined();
      expect(presets.CORE_FOCUS).toBeDefined();
      expect(presets.TERMINAL_FOCUS).toBeDefined();
      expect(presets.HOLOGRAPHIC_RECOMMENDATIONS).toBeDefined();
      expect(presets.DATA_NODES_FOCUS).toBeDefined();
      expect(presets.SAFETY_AUDIT_FOCUS).toBeDefined();

      expect(presets.NEXUS_OVERVIEW.position).toHaveLength(3);
      expect(presets.NEXUS_OVERVIEW.target).toHaveLength(3);
    });

    it('defines neural purple theme tokens', () => {
      const colors = AI_LAYOUT.colors;
      expect(colors.arenaDark).toBe('#080612');
      expect(colors.neuralPurple).toBe('#8b5cf6');
      expect(colors.neuralViolet).toBe('#c084fc');
      expect(colors.cyberCyan).toBe('#06b6d4');
      expect(colors.safetyCrimson).toBe('#dc2626');
    });
  });

  describe('2. Honest Status Disclosure ("Never pretend mock AI is production AI")', () => {
    it('explicitly declares unconfigured or mock status and disclaims production AI', () => {
      const state = useAIStore.getState();
      expect(['NOT_AVAILABLE', 'DEVELOPMENT_MOCK']).toContain(state.modelStatus);
      expect(state.isProductionAI).toBe(false);
      expect(state.advisoryOnly).toBe(true);
      expect(state.disclosureNotice).toMatch(/NOT_AVAILABLE|DEVELOPMENT MOCK/);
    });

    it('marks all recommendations as advisory-only and mock', () => {
      const recommendations = useAIStore.getState().recommendations;
      expect(recommendations.length).toBeGreaterThanOrEqual(4);

      recommendations.forEach((item) => {
        expect(item.isAdvisoryOnly).toBe(true);
        expect(item.isMock).toBe(true);
        expect(typeof item.confidenceScore).toBe('number');
        expect(item.confidenceScore).toBeGreaterThan(0);
      });
    });
  });

  describe('3. Multi-Category Intelligence Capabilities', () => {
    it('covers all required advisory categories', () => {
      const recommendations = useAIStore.getState().recommendations;
      const categories = recommendations.map((r) => r.category);

      expect(categories).toContain('RECOMMENDATION');
      expect(categories).toContain('INSIGHT');
      expect(categories).toContain('ANOMALY_INDICATOR');
      expect(categories).toContain('FAIRNESS_SUGGESTION');
    });

    it('switches active category cleanly and updates selected recommendation', () => {
      const store = useAIStore.getState();
      store.setActiveCategory('ANOMALY_INDICATOR');

      const updated = useAIStore.getState();
      expect(updated.activeCategory).toBe('ANOMALY_INDICATOR');
      expect(updated.selectedRecommendationId).toBe('ANOM_BATTERY_THERMAL');
    });

    it('provides 4 specialized satellite data nodes with live metrics', () => {
      const nodes = useAIStore.getState().dataNodes;
      expect(nodes).toHaveLength(4);

      const nodeIds = nodes.map((n) => n.id);
      expect(nodeIds).toContain('NODE_MOBILITY');
      expect(nodeIds).toContain('NODE_FAIRNESS');
      expect(nodeIds).toContain('NODE_FINANCIAL');
      expect(nodeIds).toContain('NODE_GOVERNANCE');

      nodes.forEach((n) => {
        expect(n.metrics.length).toBeGreaterThan(0);
      });
    });
  });

  describe('4. AI Safety Boundaries (BR-AI-SAFE-01 Enforcement)', () => {
    const prohibitedActions: AISafetyActionType[] = [
      'AUTHORIZE_PAYMENT',
      'ALTER_OWNERSHIP',
      'BYPASS_RBAC',
      'APPROVE_CONTRACT',
      'IRREVERSIBLE_FINANCIAL_ACTION',
    ];

    it.each(prohibitedActions)('strictly denies autonomous execution of %s', (action) => {
      expect(AISafetyEnforcer.canAIExecuteAutonomously(action)).toBe(false);
    });

    it('creates immutable audit logs and triggers safety interlock when prohibited action is attempted', () => {
      const store = useAIStore.getState();
      expect(store.lastBlockedAttempt).toBeNull();

      // Simulate an attempt to trigger automated payment via AI
      store.triggerSafetyAttempt('AUTHORIZE_PAYMENT');

      const updated = useAIStore.getState();
      expect(updated.lastBlockedAttempt).not.toBeNull();
      expect(updated.lastBlockedAttempt?.attemptedAction).toBe('AUTHORIZE_PAYMENT');
      expect(updated.lastBlockedAttempt?.blockedReason).toContain('VIOLATION: AI is strictly advisory');
      expect(updated.lastBlockedAttempt?.requiredAuthority).toBe('HUMAN_CO_OWNER_BIOMETRIC_SIGNATURE');
      expect(updated.blockedAttemptsHistory.length).toBe(1);

      // Dismiss the interlock
      store.clearBlockedAttempt();
      expect(useAIStore.getState().lastBlockedAttempt).toBeNull();
      // History is preserved
      expect(useAIStore.getState().blockedAttemptsHistory.length).toBe(1);
    });

    it('blocks ownership alterations and contract signings', () => {
      const store = useAIStore.getState();
      store.triggerSafetyAttempt('ALTER_OWNERSHIP');

      const state1 = useAIStore.getState();
      expect(state1.lastBlockedAttempt?.blockedReason).toContain('modify cap tables');

      store.triggerSafetyAttempt('APPROVE_CONTRACT');
      const state2 = useAIStore.getState();
      expect(state2.lastBlockedAttempt?.blockedReason).toContain('verifiable personal digital signatures');
    });
  });

  describe('5. Interactive Query Synthesis', () => {
    it('synthesizes query responses in advisory mode without autonomous changes', async () => {
      const store = useAIStore.getState();
      expect(store.queryResult).toBeNull();

      const queryPromise = store.runInteractiveQuery('ANALYZE SYNDICATE FAIRNESS');
      expect(useAIStore.getState().isAnalyzing).toBe(true);

      await queryPromise;

      const updated = useAIStore.getState();
      expect(updated.isAnalyzing).toBe(false);
      expect(updated.queryResult).toContain('ANALYSIS COMPLETE');
      expect(updated.queryResult).toContain('Le Hoang');
    });
  });
});
