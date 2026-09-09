package com.example.evshare.service.allocation;

/**
 * Supported utilization metrics for usage-based syndicate cost allocation per BR-FIN-02:
 * - DISTANCE: Pro-rata by logged kilometers (endOdometer - startOdometer)
 * - DURATION: Pro-rata by operating duration in hours/minutes (checkOutTime - checkInTime)
 */
public enum UsageMetric {
    DISTANCE,
    DURATION
}
