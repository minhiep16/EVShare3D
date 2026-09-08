package com.example.evshare.dto.request;

import java.math.BigDecimal;

/**
 * Deterministic input payload for standalone fair usage evaluation.
 */
public class FairUsageInput {

    private Long userId;
    private BigDecimal ownershipPercentage;
    private int bookingFrequency;
    private BigDecimal bookingDurationHours;
    private BigDecimal distanceKm;
    private int cancellations;
    private int lateCancellations;
    private BigDecimal peakHours;
    private BigDecimal standardHours;
    private BigDecimal offPeakHours;

    public FairUsageInput() {
    }

    public FairUsageInput(Long userId,
                          BigDecimal ownershipPercentage,
                          int bookingFrequency,
                          BigDecimal bookingDurationHours,
                          BigDecimal distanceKm,
                          int cancellations,
                          int lateCancellations,
                          BigDecimal peakHours,
                          BigDecimal standardHours,
                          BigDecimal offPeakHours) {
        this.userId = userId;
        this.ownershipPercentage = ownershipPercentage;
        this.bookingFrequency = bookingFrequency;
        this.bookingDurationHours = bookingDurationHours;
        this.distanceKm = distanceKm;
        this.cancellations = cancellations;
        this.lateCancellations = lateCancellations;
        this.peakHours = peakHours;
        this.standardHours = standardHours;
        this.offPeakHours = offPeakHours;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private BigDecimal ownershipPercentage = BigDecimal.ZERO;
        private int bookingFrequency = 0;
        private BigDecimal bookingDurationHours = BigDecimal.ZERO;
        private BigDecimal distanceKm = BigDecimal.ZERO;
        private int cancellations = 0;
        private int lateCancellations = 0;
        private BigDecimal peakHours = BigDecimal.ZERO;
        private BigDecimal standardHours = BigDecimal.ZERO;
        private BigDecimal offPeakHours = BigDecimal.ZERO;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder ownershipPercentage(BigDecimal ownershipPercentage) {
            this.ownershipPercentage = ownershipPercentage;
            return this;
        }

        public Builder bookingFrequency(int bookingFrequency) {
            this.bookingFrequency = bookingFrequency;
            return this;
        }

        public Builder bookingDurationHours(BigDecimal bookingDurationHours) {
            this.bookingDurationHours = bookingDurationHours;
            return this;
        }

        public Builder distanceKm(BigDecimal distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public Builder cancellations(int cancellations) {
            this.cancellations = cancellations;
            return this;
        }

        public Builder lateCancellations(int lateCancellations) {
            this.lateCancellations = lateCancellations;
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

        public FairUsageInput build() {
            return new FairUsageInput(userId, ownershipPercentage, bookingFrequency, bookingDurationHours,
                    distanceKm, cancellations, lateCancellations, peakHours, standardHours, offPeakHours);
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getOwnershipPercentage() {
        return ownershipPercentage;
    }

    public void setOwnershipPercentage(BigDecimal ownershipPercentage) {
        this.ownershipPercentage = ownershipPercentage;
    }

    public int getBookingFrequency() {
        return bookingFrequency;
    }

    public void setBookingFrequency(int bookingFrequency) {
        this.bookingFrequency = bookingFrequency;
    }

    public BigDecimal getBookingDurationHours() {
        return bookingDurationHours;
    }

    public void setBookingDurationHours(BigDecimal bookingDurationHours) {
        this.bookingDurationHours = bookingDurationHours;
    }

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public int getCancellations() {
        return cancellations;
    }

    public void setCancellations(int cancellations) {
        this.cancellations = cancellations;
    }

    public int getLateCancellations() {
        return lateCancellations;
    }

    public void setLateCancellations(int lateCancellations) {
        this.lateCancellations = lateCancellations;
    }

    public BigDecimal getPeakHours() {
        return peakHours;
    }

    public void setPeakHours(BigDecimal peakHours) {
        this.peakHours = peakHours;
    }

    public BigDecimal getStandardHours() {
        return standardHours;
    }

    public void setStandardHours(BigDecimal standardHours) {
        this.standardHours = standardHours;
    }

    public BigDecimal getOffPeakHours() {
        return offPeakHours;
    }

    public void setOffPeakHours(BigDecimal offPeakHours) {
        this.offPeakHours = offPeakHours;
    }
}
