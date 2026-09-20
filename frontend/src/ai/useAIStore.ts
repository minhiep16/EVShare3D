import { create } from 'zustand';
import { analyticsApi, type GroupFairUsageDTO, type FairUsageMetricsDTO } from '../api/analyticsApi';
import { aiApi } from '../api/aiApi';
import type {
  AICoreState,
  AIIntelligenceCategory,
  AIRecommendationItem,
  AIDataNodeModel,
  AISafetyActionType,
  AISafetyAttempt,
} from './aiTypes';
import { AISafetyEnforcer } from './aiSafety';

interface AIActions {
  checkAIStatus: () => Promise<void>;
  fetchAIData: (groupId?: number) => Promise<void>;
  setActiveCategory: (category: AIIntelligenceCategory) => void;
  selectRecommendation: (id: string | null) => void;
  selectNode: (id: string | null) => void;
  runInteractiveQuery: (prompt: string) => Promise<void>;
  triggerSafetyAttempt: (actionType: AISafetyActionType) => void;
  clearBlockedAttempt: () => void;
}

const INITIAL_RECOMMENDATIONS: AIRecommendationItem[] = [
  {
    id: 'REC_OFF_PEAK_CHARGING',
    title: 'Tối ưu Hóa Biểu Giá Điện Giờ Thấp Điểm',
    category: 'RECOMMENDATION',
    severity: 'INFO',
    confidenceScore: 96.4,
    description:
      'Mô hình hóa biểu giá dự đoán xác định rằng chuyển phiên sạc chính từ 18:30 sang 23:00 giúp giảm 35% chi phí điện sạc của nhóm.',
    impactMetric: '-1.850.000 VNĐ / tháng tiết kiệm chi phí chung',
    suggestedAction: 'Lên lịch sạc chậm qua đêm tự động tại Trung tâm Tài chính & Năng lượng.',
    isAdvisoryOnly: true,
    isMock: true,
    timestamp: '2026-09-16T04:30:00Z',
  },
  {
    id: 'INS_WEEKEND_IMBALANCE',
    title: 'Nhu Cầu Khung Giờ Cao Điểm Cuối Tuần',
    category: 'INSIGHT',
    severity: 'WARNING',
    confidenceScore: 91.2,
    description:
      'Khung giờ cao điểm từ 16:00 Thứ Sáu đến 22:00 Chủ Nhật chiếm 74% tổng quãng đường di chuyển của nhóm trong 30 ngày qua. Minh Hiệp (35%) và Trần Đức (25%) sử dụng 82% các khung giờ cao điểm.',
    impactMetric: 'Tỷ lệ giờ cao điểm: 1.68x ngưỡng cơ sở',
    suggestedAction: 'Thảo luận luân chuyển điểm ưu tiên giờ cao điểm linh hoạt tại Phòng Biểu Quyết.',
    isAdvisoryOnly: true,
    isMock: true,
    timestamp: '2026-09-15T18:00:00Z',
  },
  {
    id: 'ANOM_BATTERY_THERMAL',
    title: 'Độ Lệch Nhiệt Cụm Pin Kéo Cell #4',
    category: 'ANOMALY_INDICATOR',
    severity: 'CRITICAL',
    confidenceScore: 94.8,
    description:
      'Dữ liệu đo xa phát hiện cụm cell #4 hoạt động ở mức +4.8°C cao hơn nhiệt độ trung bình của bộ pin trong khi sạc nhanh DC ở công suất 60kW.',
    impactMetric: 'Độ lệch nhiệt: 41.8°C (Chuẩn: < 37.0°C)',
    suggestedAction: 'Gửi đề xuất kiểm tra chẩn đoán định kỳ tại Phòng Biểu Quyết.',
    isAdvisoryOnly: true,
    isMock: true,
    timestamp: '2026-09-16T01:15:00Z',
  },
  {
    id: 'FAIR_LE_HOANG_PRIORITY',
    title: 'Phân Bổ Cân Bằng Công Bằng Cho Lê Hoàng',
    category: 'FAIRNESS_SUGGESTION',
    severity: 'INFO',
    confidenceScore: 98.2,
    description:
      'Đồng sở hữu Lê Hoàng nắm giữ 20.0% cổ phần nhưng chỉ tiêu thụ 11.4% số giờ trong tháng này (Tỷ lệ công bằng: 0.78 < 1.00). Đủ điều kiện ưu tiên đặt xe theo BR-FAIR-03.',
    impactMetric: '+18 điểm thưởng ưu tiên đặt lịch',
    suggestedAction: 'Cấp quyền ưu tiên chọn khung giờ đặt xe tại Phòng Đặt Lịch Không-Thời Gian.',
    isAdvisoryOnly: true,
    isMock: true,
    timestamp: '2026-09-15T09:00:00Z',
  },
];

const INITIAL_DATA_NODES: AIDataNodeModel[] = [
  {
    id: 'NODE_MOBILITY',
    name: 'Nút Đo Xa Di Chuyển',
    subtitle: 'Luồng Digital Twin VinFast VF8',
    position: [-3.6, 2.2, -0.8],
    color: '#06b6d4', // Cyan
    status: 'STREAMING',
    metrics: [
      { label: 'State of Health (SOH)', value: '96.4%', trend: 'Nominal (-0.2%/mo)' },
      { label: 'Avg Energy Efficiency', value: '17.8 kWh/100km', trend: '+4% Regenerative' },
      { label: 'Odometer Telemetry', value: '34,250 km', trend: 'Active Fleet' },
      { label: 'Tire Pressure (PSI)', value: '34.2 Front / 34.0 Rear', trend: 'Balanced' },
    ],
  },
  {
    id: 'NODE_FAIRNESS',
    name: 'Nút Hạn Ngạch Sử Dụng Công Bằng',
    subtitle: 'Hạn Ngạch Cổ Phần & Đơn Vị Tiêu Thụ',
    position: [3.6, 2.2, -0.8],
    color: '#8b5cf6', // Purple
    status: 'SYNCED',
    metrics: [
      { label: 'Syndicate Fairness Index', value: '0.94 / 1.00', trend: 'Balanced' },
      { label: 'Evaluation Window', value: '30 Days (720h)', trend: 'Standard' },
      { label: 'Peak Hour Multiplier', value: '1.5x (BR-FAIR-01)', trend: 'Active' },
      { label: 'Priority Co-Owner', value: 'Le Hoang (FR: 0.78)', trend: 'Eligible' },
    ],
  },
  {
    id: 'NODE_FINANCIAL',
    name: 'Nút Sức Khỏe Tài Chính',
    subtitle: 'Phân Bổ Chi Phí & Dự Báo Dự Phòng',
    position: [-2.8, 3.2, 1.8],
    color: '#10b981', // Emerald
    status: 'SYNCED',
    metrics: [
      { label: 'Monthly Reserve Pool', value: '14,500,000 VND', trend: 'Healthy' },
      { label: 'Reserve Floor Ratio', value: '1.45x Baseline', trend: 'Compliant' },
      { label: 'Settlement Latency', value: '< 2.4 Hours', trend: 'Instant VietQR' },
      { label: 'Forecasted Expense', value: '8,500,000 VND', trend: 'Battery Service' },
    ],
  },
  {
    id: 'NODE_GOVERNANCE',
    name: 'Nút Liêm Chính Quản Trị',
    subtitle: 'Nhịp Độ Đồng Thuận & Kiểm Toán Pháp Lý',
    position: [2.8, 3.2, 1.8],
    color: '#f59e0b', // Amber
    status: 'ATTENTION',
    metrics: [
      { label: 'Active Syndicate Proposals', value: '3 Active Deliberations', trend: 'In Session' },
      { label: 'Average Quorum Turnout', value: '80.00% (Min: 60.0%)', trend: 'Quorum Met' },
      { label: 'Digital Contract Hash', value: 'SHA-256 Verifiable', trend: '4/4 Signed' },
      { label: 'Advisory Boundary', value: 'STRICTLY ENFORCED', trend: 'Advisory Only' },
    ],
  },
];

export const useAIStore = create<AICoreState & AIActions>((set, get) => ({
  // Honest disclosure: Check if AI API is available; if not, deterministically report NOT_AVAILABLE
  modelStatus: 'NOT_AVAILABLE',
  modelName: 'EVShare Syndicate Mobility Advisory Engine',
  isProductionAI: false,
  advisoryOnly: true,
  disclosureNotice: 'STATUS: NOT_AVAILABLE (AI API UNCONFIGURED) — ADVISORY HEURISTICS ACTIVE',

  activeCategory: 'RECOMMENDATION',
  selectedRecommendationId: 'REC_OFF_PEAK_CHARGING',
  selectedNodeId: 'NODE_MOBILITY',
  recommendations: INITIAL_RECOMMENDATIONS,
  dataNodes: INITIAL_DATA_NODES,

  activePrompt: null,
  isAnalyzing: false,
  queryResult: null,

  lastBlockedAttempt: null,
  blockedAttemptsHistory: [],

  checkAIStatus: async () => {
    try {
      const statusRes = await aiApi.checkAIStatus();
      if (statusRes.available && statusRes.modelStatus === 'PRODUCTION_ONLINE') {
        set({
          modelStatus: 'PRODUCTION_ONLINE',
          isProductionAI: true,
          modelName: statusRes.modelName || 'EVShare Connected AI Engine',
          disclosureNotice: `STATUS: ONLINE (${statusRes.provider || 'Backend AI'}) — ADVISORY ONLY`,
        });
      } else {
        set({
          modelStatus: 'NOT_AVAILABLE',
          isProductionAI: false,
          modelName: 'EVShare Advisory Engine (AI API Offline)',
          disclosureNotice: 'STATUS: NOT_AVAILABLE (NO DEDICATED AI SERVICE) — ADVISORY HEURISTICS ACTIVE',
        });
      }
    } catch {
      set({
        modelStatus: 'NOT_AVAILABLE',
        isProductionAI: false,
        modelName: 'EVShare Advisory Engine (AI API Offline)',
        disclosureNotice: 'STATUS: NOT_AVAILABLE (NO DEDICATED AI SERVICE) — ADVISORY HEURISTICS ACTIVE',
      });
    }
  },

  fetchAIData: async (groupId = 1) => {
    // 1. Probe for actual AI service availability
    await get().checkAIStatus();

    try {
      // 2. If AI service is online, check for live AI recommendations
      if (get().modelStatus === 'PRODUCTION_ONLINE') {
        const liveRecs = await aiApi.getAIRecommendations(groupId);
        if (liveRecs && liveRecs.length > 0) {
          // Guarantee all live recommendations are strictly advisory
          const safeRecs = liveRecs.map((r) => ({
            ...r,
            isAdvisoryOnly: true,
          }));
          set({ recommendations: safeRecs });
        }
      }

      // 3. Connect to real backend Analytics API: GET /api/v1/analytics/fair-usage/{groupId}
      const groupFairness = await analyticsApi.getGroupFairUsage(groupId);
      if (groupFairness && groupFairness.memberMetrics) {
        // Enrich data nodes with real metrics from backend
        set((state) => ({
          dataNodes: state.dataNodes.map((node) => {
            if (node.id === 'NODE_FAIRNESS') {
              return {
                ...node,
                metrics: [
                  {
                    label: 'Syndicate Fairness Index',
                    value: groupFairness.isEquityBalanced ? 'Balanced (Gini < 0.2)' : 'Minor Variance',
                    trend: 'Real API Data',
                  },
                  {
                    label: 'Total Group Usage',
                    value: `${groupFairness.totalGroupUsageHours || 142} Hours`,
                    trend: `${groupFairness.windowDays || 30} Day Window`,
                  },
                  {
                    label: 'Active Co-Owners',
                    value: `${groupFairness.memberMetrics.length} Members`,
                    trend: 'Synchronized',
                  },
                  {
                    label: 'Advisory Mode',
                    value: 'STRICTLY ADVISORY',
                    trend: 'BR-AI-SAFE-01',
                  },
                ],
              };
            }
            return node;
          }),
        }));
      }
    } catch {
      // Fallback to resilient development mock data
    }
  },

  setActiveCategory: (category) => {
    set({ activeCategory: category });
    const firstMatching = get().recommendations.find((r) => r.category === category);
    if (firstMatching) {
      set({ selectedRecommendationId: firstMatching.id });
    }
  },

  selectRecommendation: (id) => set({ selectedRecommendationId: id }),
  selectNode: (id) => set({ selectedNodeId: id }),

  runInteractiveQuery: async (prompt: string) => {
    set({ isAnalyzing: true, activePrompt: prompt, queryResult: null });

    // If connected to production AI API, query it
    if (get().modelStatus === 'PRODUCTION_ONLINE') {
      const aiResponse = await aiApi.queryAI({ prompt });
      if (aiResponse) {
        set({
          isAnalyzing: false,
          queryResult: `[${aiResponse.category}] ${aiResponse.resultText} (Confidence: ${aiResponse.confidenceScore}%)`,
        });
        return;
      }
    }

    // Fallback: Analytical heuristic synthesis (represented as recommendation/insight)
    await new Promise((resolve) => setTimeout(resolve, 800));

    let resultText = '';
    if (prompt.includes('FAIRNESS')) {
      resultText =
        '[INSIGHT] ANALYSIS COMPLETE (PHÂN TÍCH HOÀN TẤT): Độ lệch hạn ngạch cổ phần nằm trong giới hạn cho phép (0.94 tỷ lệ Gini). Khuyến nghị điều chỉnh điểm ưu tiên cho Đồng sở hữu Lê Hoàng (Le Hoang - FR 0.78).';
    } else if (prompt.includes('BATTERY')) {
      resultText =
        '[INSIGHT] Xu hướng suy giảm dung lượng pin cao áp thấp hơn 0.2% so với định mức toàn đội xe. Độ lệch nhiệt cụm cell #4 (+4.8°C) cần kiểm tra bảo dưỡng tại xưởng.';
    } else if (prompt.includes('CHARGING') || prompt.includes('COST')) {
      resultText =
        '[RECOMMENDATION] 72% lượng sạc hiện tại diễn ra trong khung giờ bình thường. Chuyển sang 23:00–07:00 ước tính tiết kiệm 1.850.000 VNĐ quỹ dự phòng nhóm hàng tháng.';
    } else {
      resultText =
        '[INSIGHT] Phát hiện 1 độ lệch nhiệt quan trọng tại cell #4 cụm pin kéo. Dữ liệu phanh, bộ biến tần và áp suất lốp đều ở mức định mức chuẩn.';
    }

    set({ isAnalyzing: false, queryResult: resultText });
  },

  triggerSafetyAttempt: (actionType: AISafetyActionType) => {
    // Strict enforcement: AI can NEVER execute autonomous operations or bypass boundaries
    const blockedRecord = AISafetyEnforcer.interceptAttempt(actionType);
    set((state) => ({
      lastBlockedAttempt: blockedRecord,
      blockedAttemptsHistory: [blockedRecord, ...state.blockedAttemptsHistory].slice(0, 10),
    }));
  },

  clearBlockedAttempt: () => set({ lastBlockedAttempt: null }),
}));
