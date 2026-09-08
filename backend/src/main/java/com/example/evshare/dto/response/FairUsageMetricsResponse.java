package com.example.evshare.dto.response;

import com.example.evshare.entity.enums.ImbalanceLevel;

import java.math.BigDecimal;

/**
 * Detailed fair usage metrics and recommendations for an individual co-owner.
 */
public class FairUsageMetricsResponse {

    private Long userId;
    private String fullName;
    private BigDecimal ownershipPercentage;
    private BigDecimal equityQuotaHours;
    private int bookingFrequency;
    private BigDecimal totalDurationHours;
    private BigDecimal totalDistanceKm;
    private int cancellationCount;
    private int lateCancellationCount;
    private BigDecimal peakHours;
    private BigDecimal standardHours;
    private BigDecimal offPeakHours;
    private BigDecimal weightedUsageUnits;
    private BigDecimal fairnessRatio;
    private BigDecimal fairnessScore;
    private ImbalanceLevel imbalanceLevel;
    private boolean isPriorityBookingEligible;
    private boolean isPeakRestricted;
    private String recommendation;

    public FairUsageMetricsResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String fullName;
        private BigDecimal ownershipPercentage;
        private BigDecimal equityQuotaHours;
        private int bookingFrequency;
        private BigDecimal totalDurationHours;
        private BigDecimal totalDistanceKm;
        private int cancellationCount;
        private int lateCancellationCount;
        private BigDecimal peakHours;
        private BigDecimal standardHours;
        private BigDecimal offPeakHours;
        private BigDecimal weightedUsageUnits;
        private BigDecimal fairnessRatio;
        private BigDecimal fairnessScore;
        private ImbalanceLevel imbalanceLevel;
        private boolean isPriorityBookingEligible;
        private boolean isPeakRestricted;
        private String recommendation;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder ownershipPercentage(BigDecimal ownershipPercentage) {
            this.ownershipPercentage = ownershipPercentage;
            return this;
        }

        public Builder equityQuotaHours(BigDecimal equityQuotaHours) {
            this.equityQuotaHours = equityQuotaHours;
            return this;
        }

        public Builder bookingFrequency(int bookingFrequency) {
            this.bookingFrequency = bookingFrequency;
            return this;
        }

        public Builder totalDurationHours(BigDecimal totalDurationHours) {
            this.totalDurationHours = totalDurationHours;
            return this;
        }

        public Builder totalDistanceKm(BigDecimal totalDistanceKm) {
            this.totalDistanceKm = totalDistanceKm;
            return this;
        }

        public Builder cancellationCount(int cancellationCount) {
            this.cancellationCount = cancellationCount;
            return this;
        }

        public Builder lateCancellationCount(int lateCancellationCount) {
            this.lateCancellationCount = lateCancellationCount;
            return this;
        }

        public Builder peakHours(BigDecimal peakHours) {
            this.peakHours = peakHours;
            return this;
        }

        public Builder standardHours(BigDecimal standardHours) {
            this.standardHours = standardHours;
            return this;
        }

        public Builder offPeakHours(BigDecimal offPeakHours) {
            this.offPeakHours = offPeakHours;
            return this;
        }

        public Builder weightedUsageUnits(BigDecimal weightedUsageUnits) {
            this.weightedUsageUnits = weightedUsageUnits;
            return this;
        }

        public Builder fairnessRatio(BigDecimal fairnessRatio) {
            this.fairnessRatio = fairnessRatio;
            return this;
        }

        public Builder fairnessScore(BigDecimal fairnessScore) {
            this.fairnessScore = fairnessScore;
            return this;
        }

        public Builder imbalanceLevel(ImbalanceLevel imbalanceLevel) {
            this.imbalanceLevel = imbalanceLevel;
            return this;
        }

        public Builder isPriorityBookingEligible(boolean isPriorityBookingEligible) {
            this.isPriorityBookingEligible = isPriorityBookingEligible;
            return this;
        }

        public Builder isPeakRestricted(boolean isPeakRestricted) {
            this.isPeakRestricted = isPeakRestricted;
            return this;
        }

        public Builder recommendation(String recommendation) {
            this.recommendation = recommendation;
            return this;
        }

        public FairUsageMetricsResponse build() {
            FairUsageMetricsResponse r = new FairUsageMetricsResponse();
            r.userId = this.userId;
            r.fullName = this.fullName;
            r.ownershipPercentage = this.ownershipPercentage;
            r.equityQuotaHours = this.equityQuotaHours;
            r.bookingFrequency = this.bookingFrequency;
            r.totalDurationHours = this.totalDurationHours;
            r.totalDistanceKm = this.totalDistanceKm;
            r.cancellationCount = this.cancellationCount;
            r.lateCancellationCount = this.lateCancellationCount;
            r.peakHours = this.peakHours;
            r.standardHours = this.standardHours;
            r.offPeakHours = this.offPeakHours;
            r.weightedUsageUnits = this.weightedUsageUnits;
            r.fairnessRatio = this.fairnessRatio;
            r.fairnessScore = this.fairnessScore;
            r.imbalanceLevel = this.imbalanceLevel;
            r.isPriorityBookingEligible = this.isPriorityBookingEligible;
            r.isPeakRestricted = this.isPeakRestricted;
            r.recommendation = this.recommendation;
            return r;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public BigDecimal getOwnershipPercentage() {
        return ownershipPercentage;
    }

    public BigDecimal getEquityQuotaHours() {
        return equityQuotaHours;
    }

    public int getBookingFrequency() {
        return bookingFrequency;
    }

    public BigDecimal getTotalDurationHours() {
        return totalDurationHours;
    }

    public BigDecimal getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public int getCancellationCount() {
        return cancellationCount;
    }

    public int getLateCancellationCount() {
        return lateCancellationCount;
    }

    public BigDecimal getPeakHours() {
        return peakHours;
    }

    public BigDecimal getStandardHours() {
        return standardHours;
    }

    public BigDecimal getOffPeakHours() {
        return offPeakHours;
    }

    public BigDecimal getWeightedUsageUnits() {
        return weightedUsageUnits;
    }

    public BigDecimal getFairnessRatio() {
        return fairnessRatio;
    }

    public BigDecimal getFairnessScore() {
        return fairnessScore;
    }

    public ImbalanceLevel getImbalanceLevel() {
        return imbalanceLevel;
    }

    public boolean isPriorityBookingEligible() {
        return isPriorityBookingEligible;
    }

    public boolean isPeakRestricted() {
        return isPeakRestricted;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
