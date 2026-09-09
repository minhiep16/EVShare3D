package com.example.evshare.entity.enums;

public enum ExpenseCategory {
    CHARGING,
    MAINTENANCE,
    INSURANCE,
    INSPECTION,
    CLEANING,
    REPAIR,
    PARKING,
    TOLL,
    OTHER,
    PREVENTIVE_MAINTENANCE,
    EMERGENCY_REPAIR;

    /**
     * Determines whether receipt or invoice evidence is mandatory for this category.
     */
    public boolean isEvidenceRequired() {
        return this == MAINTENANCE
                || this == PREVENTIVE_MAINTENANCE
                || this == REPAIR
                || this == EMERGENCY_REPAIR
                || this == INSURANCE
                || this == INSPECTION;
    }

    /**
     * Identifies fixed overhead expenses that require OWNERSHIP_BASED allocation under BR-FIN-01.
     */
    public boolean isFixedOverhead() {
        return this == INSURANCE || this == INSPECTION;
    }
}
