package com.example.evshare.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Syndicate-wide fair usage report containing metrics for all co-owners.
 */
public class GroupFairUsageResponse {

    private Long groupId;
    private String groupName;
    private Long vehicleId;
    private String vehicleModel;
    private int evaluationWindowDays;
    private BigDecimal totalAvailableHours;
    private BigDecimal totalGroupUsageHours;
    private BigDecimal totalGroupWeightedUnits;
    private List<FairUsageMetricsResponse> memberMetrics = new ArrayList<>();

    public GroupFairUsageResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long groupId;
        private String groupName;
        private Long vehicleId;
        private String vehicleModel;
        private int evaluationWindowDays;
        private BigDecimal totalAvailableHours;
        private BigDecimal totalGroupUsageHours;
        private BigDecimal totalGroupWeightedUnits;
        private List<FairUsageMetricsResponse> memberMetrics = new ArrayList<>();

        public Builder groupId(Long groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder vehicleId(Long vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public Builder vehicleModel(String vehicleModel) {
            this.vehicleModel = vehicleModel;
            return this;
        }

        public Builder evaluationWindowDays(int evaluationWindowDays) {
            this.evaluationWindowDays = evaluationWindowDays;
            return this;
        }

        public Builder totalAvailableHours(BigDecimal totalAvailableHours) {
            this.totalAvailableHours = totalAvailableHours;
            return this;
        }

        public Builder totalGroupUsageHours(BigDecimal totalGroupUsageHours) {
            this.totalGroupUsageHours = totalGroupUsageHours;
            return this;
        }

        public Builder totalGroupWeightedUnits(BigDecimal totalGroupWeightedUnits) {
            this.totalGroupWeightedUnits = totalGroupWeightedUnits;
            return this;
        }

        public Builder memberMetrics(List<FairUsageMetricsResponse> memberMetrics) {
            this.memberMetrics = memberMetrics;
            return this;
        }

        public GroupFairUsageResponse build() {
            GroupFairUsageResponse r = new GroupFairUsageResponse();
            r.groupId = this.groupId;
            r.groupName = this.groupName;
            r.vehicleId = this.vehicleId;
            r.vehicleModel = this.vehicleModel;
            r.evaluationWindowDays = this.evaluationWindowDays;
            r.totalAvailableHours = this.totalAvailableHours;
            r.totalGroupUsageHours = this.totalGroupUsageHours;
            r.totalGroupWeightedUnits = this.totalGroupWeightedUnits;
            r.memberMetrics = this.memberMetrics;
            return r;
        }
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public int getEvaluationWindowDays() {
        return evaluationWindowDays;
    }

    public BigDecimal getTotalAvailableHours() {
        return totalAvailableHours;
    }

    public BigDecimal getTotalGroupUsageHours() {
        return totalGroupUsageHours;
    }

    public BigDecimal getTotalGroupWeightedUnits() {
        return totalGroupWeightedUnits;
    }

    public List<FairUsageMetricsResponse> getMemberMetrics() {
        return memberMetrics;
    }
}
