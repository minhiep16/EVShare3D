import { apiClient } from './apiClient';
import type { ApiResponse } from './vehiclesApi';

export interface FairUsageMetricsDTO {
  userId: number;
  groupId: number;
  userName: string;
  userEmail: string;
  equityPercentage: number;
  actualUsageHours: number;
  fairShareHours: number;
  fairnessRatio: number;
  imbalanceLevel: 'BALANCED' | 'MODERATE_OVERUSE' | 'HIGH_OVERUSE' | 'UNDERUSE';
  bookingPriorityScore: number;
  evaluatedWindowDays: number;
  windowStartDate: string;
  windowEndDate: string;
  recommendationMessage: string;
}

export interface GroupMemberFairUsageDTO {
  userId: number;
  userName: string;
  equityPercentage: number;
  actualUsageHours: number;
  fairShareHours: number;
  fairnessRatio: number;
  imbalanceLevel: string;
  bookingPriorityScore: number;
}

export interface GroupFairUsageDTO {
  groupId: number;
  groupName: string;
  totalGroupUsageHours: number;
  windowDays: number;
  windowStartDate: string;
  windowEndDate: string;
  giniCoefficient: number;
  isEquityBalanced: boolean;
  memberMetrics: GroupMemberFairUsageDTO[];
}

export const analyticsApi = {
  /**
   * Retrieves current authenticated user's fair usage score and priority recommendation.
   */
  getMyFairUsageScore: async (
    groupId: number,
    windowDays = 30
  ): Promise<FairUsageMetricsDTO> => {
    const res = await apiClient.get<ApiResponse<FairUsageMetricsDTO>>(
      `/analytics/fair-usage/${groupId}/my-score`,
      { params: { windowDays } }
    );
    return res.data.data;
  },

  /**
   * Retrieves syndicate-wide fair usage analytics and Gini inequality metrics.
   */
  getGroupFairUsage: async (
    groupId: number,
    windowDays = 30
  ): Promise<GroupFairUsageDTO> => {
    const res = await apiClient.get<ApiResponse<GroupFairUsageDTO>>(
      `/analytics/fair-usage/${groupId}`,
      { params: { windowDays } }
    );
    return res.data.data;
  },
};
