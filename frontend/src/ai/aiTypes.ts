export type AIModelStatus = 'PRODUCTION_ONLINE' | 'DEVELOPMENT_MOCK' | 'NOT_AVAILABLE';

export type AIIntelligenceCategory =
  | 'RECOMMENDATION'
  | 'INSIGHT'
  | 'ANOMALY_INDICATOR'
  | 'FAIRNESS_SUGGESTION';

export type AnomalySeverity = 'NORMAL' | 'INFO' | 'WARNING' | 'CRITICAL';

export type AISafetyActionType =
  | 'BYPASS_AUTHENTICATION'
  | 'BYPASS_RBAC'
  | 'ALTER_OWNERSHIP'
  | 'AUTHORIZE_PAYMENT'
  | 'APPROVE_CONTRACT'
  | 'OVERRIDE_VOTING_RULES'
  | 'IRREVERSIBLE_FINANCIAL_ACTION';

export interface AISafetyAttempt {
  id: string;
  attemptedAction: AISafetyActionType;
  actionLabel: string;
  blockedReason: string;
  timestamp: string;
  requiredAuthority: string;
}

export interface AIRecommendationItem {
  id: string;
  title: string;
  category: AIIntelligenceCategory;
  severity: AnomalySeverity;
  confidenceScore: number; // e.g. 94.5%
  description: string;
  impactMetric: string;
  suggestedAction: string;
  isAdvisoryOnly: boolean;
  isMock: boolean;
  timestamp: string;
}

export interface AIDataNodeModel {
  id: 'NODE_MOBILITY' | 'NODE_FAIRNESS' | 'NODE_FINANCIAL' | 'NODE_GOVERNANCE';
  name: string;
  subtitle: string;
  position: [number, number, number];
  color: string;
  status: 'STREAMING' | 'SYNCED' | 'ATTENTION';
  metrics: { label: string; value: string; trend?: string }[];
}

export interface AICameraPreset {
  name: string;
  position: [number, number, number];
  target: [number, number, number];
}

export interface AICoreState {
  // Engine and Honest Disclosure metadata
  modelStatus: AIModelStatus;
  modelName: string;
  isProductionAI: boolean;
  advisoryOnly: boolean;
  disclosureNotice: string;

  // Intelligence content
  activeCategory: AIIntelligenceCategory;
  selectedRecommendationId: string | null;
  selectedNodeId: string | null;
  recommendations: AIRecommendationItem[];
  dataNodes: AIDataNodeModel[];

  // Interactive telemetry & queries
  activePrompt: string | null;
  isAnalyzing: boolean;
  queryResult: string | null;

  // Safety Boundary Enforcement
  lastBlockedAttempt: AISafetyAttempt | null;
  blockedAttemptsHistory: AISafetyAttempt[];
}
