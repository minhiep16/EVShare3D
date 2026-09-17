import { apiClient } from './apiClient';
import type { AIModelStatus, AIRecommendationItem } from '../ai/aiTypes';

export interface AIStatusResponse {
  available: boolean;
  modelStatus: AIModelStatus;
  modelName?: string;
  provider?: string;
  version?: string;
  message?: string;
}

export interface AIQueryPayload {
  prompt: string;
  context?: Record<string, unknown>;
}

export interface AIQueryResponse {
  resultText: string;
  category: 'RECOMMENDATION' | 'INSIGHT';
  confidenceScore: number;
  isAdvisoryOnly: boolean;
  generatedAt: string;
}

export const aiApi = {
  /**
   * Probes for external or backend AI service availability.
   * If endpoint is missing, unconfigured, or returns error/404,
   * cleanly reports NOT_AVAILABLE.
   */
  checkAIStatus: async (): Promise<AIStatusResponse> => {
    try {
      const res = await apiClient.get<{ data: AIStatusResponse }>('/ai/status', {
        timeout: 3000,
      });
      if (res.data?.data) {
        return res.data.data;
      }
      return {
        available: false,
        modelStatus: 'NOT_AVAILABLE',
        message: 'AI Service unconfigured on backend',
      };
    } catch {
      // Clean deterministic fallback: report NOT_AVAILABLE
      return {
        available: false,
        modelStatus: 'NOT_AVAILABLE',
        message: 'AI API unavailable or unconfigured (Reporting NOT_AVAILABLE per BR-AI-SAFE-01)',
      };
    }
  },

  /**
   * Fetches AI-generated advisory recommendations if service is online.
   */
  getAIRecommendations: async (groupId = 1): Promise<AIRecommendationItem[]> => {
    try {
      const res = await apiClient.get<{ data: AIRecommendationItem[] }>(`/ai/recommendations/${groupId}`, {
        timeout: 4000,
      });
      return res.data?.data || [];
    } catch {
      return [];
    }
  },

  /**
   * Submits an analytical query to the AI engine for synthesis.
   * All responses are guaranteed to be advisory recommendations/insights.
   */
  queryAI: async (payload: AIQueryPayload): Promise<AIQueryResponse | null> => {
    try {
      const res = await apiClient.post<{ data: AIQueryResponse }>('/ai/query', payload, {
        timeout: 6000,
      });
      return res.data?.data || null;
    } catch {
      return null;
    }
  },
};
