package com.example.evshare.service;

import com.example.evshare.dto.request.FairUsageInput;
import com.example.evshare.dto.response.FairUsageMetricsResponse;
import com.example.evshare.dto.response.GroupFairUsageResponse;
import com.example.evshare.entity.enums.ImbalanceLevel;

import java.math.BigDecimal;

/**
 * Fair Usage Service governing vehicle allocation, demand weighting,
 * and fairness ratio calculations per BR-FAIR-01, BR-FAIR-02, and BR-FAIR-03.
 */
public interface FairUsageService {

    /**
     * Computes deterministic fair usage metrics from an input record and syndicate context.
     * Pure function: 100% unit-testable without database access.
     *
     * @param input user metrics input
     * @param totalGroupWeightedUnits total weighted units consumed across the syndicate
     * @param totalAvailableHours total hours in the evaluation window
     * @return calculated fair usage breakdown
     */
    FairUsageMetricsResponse computeMetrics(FairUsageInput input, BigDecimal totalGroupWeightedUnits, BigDecimal totalAvailableHours);

    /**
     * Calculates weighted usage consumption units (BR-FAIR-01):
     * W_i = (Peak * 1.5) + (Standard * 1.0) + (OffPeak * 0.7)
     */
    BigDecimal calculateWeightedUsage(BigDecimal peakHours, BigDecimal standardHours, BigDecimal offPeakHours);

    /**
     * Calculates equity quota in hours (BR-FAIR-01):
     * Q_i = (percentage / 100.0) * totalAvailableHours
     */
    BigDecimal calculateEquityQuota(BigDecimal ownershipPercentage, BigDecimal totalAvailableHours);

    /**
     * Calculates normalized fairness ratio (BR-FAIR-02):
     * FR_i = (userWeighted / totalGroupWeighted) / (percentage / 100.0)
     */
    BigDecimal calculateFairnessRatio(BigDecimal userWeightedUsage, BigDecimal totalGroupWeightedUsage, BigDecimal ownershipPercentage);

    /**
     * Calculates fairness score on [0.0, 100.0] scale:
     * Score_i = max(0.00, 100.00 - 100.00 * |FR_i - 1.00|)
     */
    BigDecimal calculateFairnessScore(BigDecimal fairnessRatio);

    /**
     * Classifies fairness ratio into 4 canonical tiers (BR-FAIR-02):
     * - FAIR: [0.90, 1.10]
     * - SLIGHTLY_IMBALANCED: [0.75, 0.90) or (1.10, 1.25]
     * - IMBALANCED: [0.50, 0.75) or (1.25, 1.50]
     * - SEVERELY_IMBALANCED: < 0.50 or > 1.50
     */
    ImbalanceLevel determineImbalanceLevel(BigDecimal fairnessRatio);

    /**
     * Generates transparent, deterministic recommendations without hidden weights.
     */
    String generateRecommendation(BigDecimal fairnessRatio, ImbalanceLevel level, boolean isOverUser);

    /**
     * Retrieves syndicate-wide fair usage report from database history for an evaluation window.
     *
     * @param groupId the ownership group ID
     * @param windowDays evaluation window duration in days (default 30)
     * @param currentUserId authenticated user ID
     * @param isAdmin whether caller is staff/admin
     * @return group fair usage response
     */
    GroupFairUsageResponse getGroupFairUsage(Long groupId, int windowDays, Long currentUserId, boolean isAdmin);

    /**
     * Retrieves fair usage report for a specific co-owner within an ownership group.
     *
     * @param groupId the ownership group ID
     * @param userId the target co-owner ID
     * @param windowDays evaluation window duration in days (default 30)
     * @param currentUserId authenticated user ID
     * @param isAdmin whether caller is staff/admin
     * @return user fair usage metrics
     */
    FairUsageMetricsResponse getUserFairUsage(Long groupId, Long userId, int windowDays, Long currentUserId, boolean isAdmin);
}
