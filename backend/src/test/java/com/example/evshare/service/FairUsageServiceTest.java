package com.example.evshare.service;

import com.example.evshare.dto.request.FairUsageInput;
import com.example.evshare.dto.response.FairUsageMetricsResponse;
import com.example.evshare.entity.enums.ImbalanceLevel;
import com.example.evshare.service.impl.FairUsageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Checkpoint 05-H — Fair Usage Service Unit Tests")
class FairUsageServiceTest {

    private FairUsageServiceImpl fairUsageService;

    @BeforeEach
    void setUp() {
        // Instantiate without mock repository dependencies for pure mathematical unit tests
        fairUsageService = new FairUsageServiceImpl(null, null, null, null, null);
    }

    @Test
    @DisplayName("1. Demand Tier Weighting: Accurately calculates Peak (1.5x), Standard (1.0x), and Off-Peak (0.7x)")
    void testCalculateWeightedUsage() {
        // 10h Peak (15.0) + 20h Standard (20.0) + 10h Off-Peak (7.0) = 42.00
        BigDecimal weighted = fairUsageService.calculateWeightedUsage(
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00")
        );
        assertEquals(new BigDecimal("42.00"), weighted);

        // All zero hours
        BigDecimal zero = fairUsageService.calculateWeightedUsage(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), zero);

        // Null inputs handled safely
        BigDecimal nullSafe = fairUsageService.calculateWeightedUsage(null, null, null);
        assertEquals(new BigDecimal("0.00"), nullSafe);
    }

    @Test
    @DisplayName("2. Equity Quota: Accurately calculates monthly quota hours (BR-FAIR-01)")
    void testCalculateEquityQuota() {
        BigDecimal totalHours = new BigDecimal("720.00"); // 30-day window

        // 25.00% equity -> 180.00 hours
        BigDecimal quota25 = fairUsageService.calculateEquityQuota(new BigDecimal("25.00"), totalHours);
        assertEquals(new BigDecimal("180.00"), quota25);

        // 50.00% equity -> 360.00 hours
        BigDecimal quota50 = fairUsageService.calculateEquityQuota(new BigDecimal("50.00"), totalHours);
        assertEquals(new BigDecimal("360.00"), quota50);

        // 10.00% equity -> 72.00 hours
        BigDecimal quota10 = fairUsageService.calculateEquityQuota(new BigDecimal("10.00"), totalHours);
        assertEquals(new BigDecimal("72.00"), quota10);
    }

    @Test
    @DisplayName("3. Balanced Usage Case: 4 equal co-owners with identical consumption yield FR=1.00 and Score=100.00 (FAIR)")
    void testBalancedUsageScenario() {
        BigDecimal groupTotalWeighted = new BigDecimal("200.00");
        BigDecimal totalHours = new BigDecimal("720.00");

        FairUsageInput input = FairUsageInput.builder()
                .userId(101L)
                .ownershipPercentage(new BigDecimal("25.00"))
                .bookingFrequency(5)
                .bookingDurationHours(new BigDecimal("40.00"))
                .distanceKm(new BigDecimal("500.00"))
                .peakHours(new BigDecimal("20.00"))     // 20 * 1.5 = 30.0
                .standardHours(new BigDecimal("15.00")) // 15 * 1.0 = 15.0
                .offPeakHours(new BigDecimal("7.14"))  // approx 5.0
                .build();

        // Let's set exact weighted to 50.00 (25% of 200.00): 20 * 1.5 = 30.0 + 20 * 1.0 = 20.0 + 0 = 50.0
        FairUsageInput exactBalanced = FairUsageInput.builder()
                .userId(101L)
                .ownershipPercentage(new BigDecimal("25.00"))
                .bookingFrequency(4)
                .bookingDurationHours(new BigDecimal("40.00"))
                .distanceKm(new BigDecimal("450.00"))
                .peakHours(new BigDecimal("20.00"))
                .standardHours(new BigDecimal("20.00"))
                .offPeakHours(BigDecimal.ZERO)
                .build();

        FairUsageMetricsResponse response = fairUsageService.computeMetrics(exactBalanced, groupTotalWeighted, totalHours);

        assertEquals(new BigDecimal("50.00"), response.getWeightedUsageUnits());
        assertEquals(new BigDecimal("1.00"), response.getFairnessRatio());
        assertEquals(new BigDecimal("100.00"), response.getFairnessScore());
        assertEquals(ImbalanceLevel.FAIR, response.getImbalanceLevel());
        assertFalse(response.isPriorityBookingEligible());
        assertFalse(response.isPeakRestricted());
        assertTrue(response.getRecommendation().contains("in equilibrium"));
    }

    @Test
    @DisplayName("4. Asymmetric Equity Proportional Usage: Owners with 50%, 30%, 20% all maintain equilibrium")
    void testAsymmetricEquityProportionalUsage() {
        BigDecimal groupTotalWeighted = new BigDecimal("200.00");

        // Owner A: 50% equity, consumes 100 weighted units
        BigDecimal ratioA = fairUsageService.calculateFairnessRatio(new BigDecimal("100.00"), groupTotalWeighted, new BigDecimal("50.00"));
        assertEquals(new BigDecimal("1.00"), ratioA);
        assertEquals(ImbalanceLevel.FAIR, fairUsageService.determineImbalanceLevel(ratioA));

        // Owner B: 30% equity, consumes 60 weighted units
        BigDecimal ratioB = fairUsageService.calculateFairnessRatio(new BigDecimal("60.00"), groupTotalWeighted, new BigDecimal("30.00"));
        assertEquals(new BigDecimal("1.00"), ratioB);
        assertEquals(ImbalanceLevel.FAIR, fairUsageService.determineImbalanceLevel(ratioB));

        // Owner C: 20% equity, consumes 40 weighted units
        BigDecimal ratioC = fairUsageService.calculateFairnessRatio(new BigDecimal("40.00"), groupTotalWeighted, new BigDecimal("20.00"));
        assertEquals(new BigDecimal("1.00"), ratioC);
        assertEquals(ImbalanceLevel.FAIR, fairUsageService.determineImbalanceLevel(ratioC));
    }

    @Test
    @DisplayName("5. Slightly Imbalanced Under-User: FR=0.80, Score=80.00, gains priority booking")
    void testSlightlyImbalancedUnderUser() {
        BigDecimal groupTotalWeighted = new BigDecimal("200.00");
        BigDecimal totalHours = new BigDecimal("720.00");

        // 25% equity consumes 40 weighted units (20% share): FR = 0.20 / 0.25 = 0.80
        FairUsageInput input = FairUsageInput.builder()
                .userId(102L)
                .ownershipPercentage(new BigDecimal("25.00"))
                .bookingFrequency(3)
                .bookingDurationHours(new BigDecimal("30.00"))
                .distanceKm(new BigDecimal("300.00"))
                .peakHours(new BigDecimal("10.00"))     // 15.0
                .standardHours(new BigDecimal("25.00")) // 25.0 -> total 40.0
                .offPeakHours(BigDecimal.ZERO)
                .build();

        FairUsageMetricsResponse response = fairUsageService.computeMetrics(input, groupTotalWeighted, totalHours);

        assertEquals(new BigDecimal("40.00"), response.getWeightedUsageUnits());
        assertEquals(new BigDecimal("0.80"), response.getFairnessRatio());
        assertEquals(new BigDecimal("80.00"), response.getFairnessScore());
        assertEquals(ImbalanceLevel.SLIGHTLY_IMBALANCED, response.getImbalanceLevel());
        assertTrue(response.isPriorityBookingEligible(), "Under-user with FR < 1.0 gains priority scheduling advantage");
        assertFalse(response.isPeakRestricted());
        assertTrue(response.getRecommendation().contains("priority booking"));
    }

    @Test
    @DisplayName("6. Slightly Imbalanced Over-User: FR=1.20, Score=80.00, advised to shift off-peak")
    void testSlightlyImbalancedOverUser() {
        BigDecimal groupTotalWeighted = new BigDecimal("200.00");
        BigDecimal totalHours = new BigDecimal("720.00");

        // 25% equity consumes 60 weighted units (30% share): FR = 0.30 / 0.25 = 1.20
        FairUsageInput input = FairUsageInput.builder()
                .userId(103L)
                .ownershipPercentage(new BigDecimal("25.00"))
                .bookingFrequency(6)
                .bookingDurationHours(new BigDecimal("50.00"))
                .distanceKm(new BigDecimal("600.00"))
                .peakHours(new BigDecimal("20.00"))     // 30.0
                .standardHours(new BigDecimal("30.00")) // 30.0 -> total 60.0
                .offPeakHours(BigDecimal.ZERO)
                .build();

        FairUsageMetricsResponse response = fairUsageService.computeMetrics(input, groupTotalWeighted, totalHours);

        assertEquals(new BigDecimal("60.00"), response.getWeightedUsageUnits());
        assertEquals(new BigDecimal("1.20"), response.getFairnessRatio());
        assertEquals(new BigDecimal("80.00"), response.getFairnessScore());
        assertEquals(ImbalanceLevel.SLIGHTLY_IMBALANCED, response.getImbalanceLevel());
        assertFalse(response.isPriorityBookingEligible());
        assertFalse(response.isPeakRestricted(), "FR=1.20 is below 1.30 peak restriction threshold");
        assertTrue(response.getRecommendation().contains("off-peak"));
    }

    @Test
    @DisplayName("7. Moderately Imbalanced Over-User: FR=1.35 triggers Peak Restriction (BR-FAIR-03)")
    void testModeratelyImbalancedOverUserWithPeakRestriction() {
        BigDecimal groupTotalWeighted = new BigDecimal("200.00");
        BigDecimal totalHours = new BigDecimal("720.00");

        // 25% equity consumes 67.50 weighted units (33.75% share): FR = 0.3375 / 0.25 = 1.35
        FairUsageInput input = FairUsageInput.builder()
                .userId(104L)
                .ownershipPercentage(new BigDecimal("25.00"))
                .bookingFrequency(8)
                .bookingDurationHours(new BigDecimal("60.00"))
                .distanceKm(new BigDecimal("800.00"))
                .peakHours(new BigDecimal("25.00"))     // 37.50
                .standardHours(new BigDecimal("30.00")) // 30.00 -> total 67.50
                .offPeakHours(BigDecimal.ZERO)
                .build();

        FairUsageMetricsResponse response = fairUsageService.computeMetrics(input, groupTotalWeighted, totalHours);

        assertEquals(new BigDecimal("67.50"), response.getWeightedUsageUnits());
        assertEquals(new BigDecimal("1.35"), response.getFairnessRatio());
        assertEquals(new BigDecimal("65.00"), response.getFairnessScore());
        assertEquals(ImbalanceLevel.IMBALANCED, response.getImbalanceLevel());
        assertFalse(response.isPriorityBookingEligible());
        assertTrue(response.isPeakRestricted(), "FR > 1.30 incurs peak-hour reservation restriction per BR-FAIR-03");
        assertTrue(response.getRecommendation().contains("0.7x"));
    }

    @Test
    @DisplayName("8. Severely Imbalanced Over-User: FR=1.80, Score=20.00, SEVERELY_IMBALANCED")
    void testSeverelyImbalancedOverUser() {
        BigDecimal groupTotalWeighted = new BigDecimal("200.00");
        BigDecimal totalHours = new BigDecimal("720.00");

        // 20% equity consumes 72 weighted units (36% share): FR = 0.36 / 0.20 = 1.80
        FairUsageInput input = FairUsageInput.builder()
                .userId(105L)
                .ownershipPercentage(new BigDecimal("20.00"))
                .bookingFrequency(10)
                .bookingDurationHours(new BigDecimal("70.00"))
                .distanceKm(new BigDecimal("1100.00"))
                .peakHours(new BigDecimal("30.00"))     // 45.0
                .standardHours(new BigDecimal("27.00")) // 27.0 -> total 72.0
                .offPeakHours(BigDecimal.ZERO)
                .build();

        FairUsageMetricsResponse response = fairUsageService.computeMetrics(input, groupTotalWeighted, totalHours);

        assertEquals(new BigDecimal("72.00"), response.getWeightedUsageUnits());
        assertEquals(new BigDecimal("1.80"), response.getFairnessRatio());
        assertEquals(new BigDecimal("20.00"), response.getFairnessScore());
        assertEquals(ImbalanceLevel.SEVERELY_IMBALANCED, response.getImbalanceLevel());
        assertTrue(response.isPeakRestricted());
        assertTrue(response.getRecommendation().contains("Severe over-utilization"));
    }

    @Test
    @DisplayName("9. Severely Imbalanced Under-User: FR=0.40, Score=40.00, SEVERELY_IMBALANCED")
    void testSeverelyImbalancedUnderUser() {
        BigDecimal groupTotalWeighted = new BigDecimal("200.00");
        BigDecimal totalHours = new BigDecimal("720.00");

        // 25% equity consumes 20 weighted units (10% share): FR = 0.10 / 0.25 = 0.40
        FairUsageInput input = FairUsageInput.builder()
                .userId(106L)
                .ownershipPercentage(new BigDecimal("25.00"))
                .bookingFrequency(1)
                .bookingDurationHours(new BigDecimal("15.00"))
                .distanceKm(new BigDecimal("120.00"))
                .peakHours(BigDecimal.ZERO)
                .standardHours(new BigDecimal("20.00")) // 20.0
                .offPeakHours(BigDecimal.ZERO)
                .build();

        FairUsageMetricsResponse response = fairUsageService.computeMetrics(input, groupTotalWeighted, totalHours);

        assertEquals(new BigDecimal("20.00"), response.getWeightedUsageUnits());
        assertEquals(new BigDecimal("0.40"), response.getFairnessRatio());
        assertEquals(new BigDecimal("40.00"), response.getFairnessScore());
        assertEquals(ImbalanceLevel.SEVERELY_IMBALANCED, response.getImbalanceLevel());
        assertTrue(response.isPriorityBookingEligible());
        assertFalse(response.isPeakRestricted());
        assertTrue(response.getRecommendation().contains("Severe under-utilization"));
    }

    @Test
    @DisplayName("10. Zero-Usage Syndicate: Group with 0 bookings defaults to FR=1.00, Score=100.00 (FAIR)")
    void testZeroUsageSyndicateBaseline() {
        BigDecimal groupTotalWeighted = BigDecimal.ZERO;
        BigDecimal totalHours = new BigDecimal("720.00");

        FairUsageInput input = FairUsageInput.builder()
                .userId(107L)
                .ownershipPercentage(new BigDecimal("25.00"))
                .bookingFrequency(0)
                .bookingDurationHours(BigDecimal.ZERO)
                .distanceKm(BigDecimal.ZERO)
                .peakHours(BigDecimal.ZERO)
                .standardHours(BigDecimal.ZERO)
                .offPeakHours(BigDecimal.ZERO)
                .build();

        FairUsageMetricsResponse response = fairUsageService.computeMetrics(input, groupTotalWeighted, totalHours);

        assertEquals(new BigDecimal("0.00"), response.getWeightedUsageUnits());
        assertEquals(new BigDecimal("1.00"), response.getFairnessRatio());
        assertEquals(new BigDecimal("100.00"), response.getFairnessScore());
        assertEquals(ImbalanceLevel.FAIR, response.getImbalanceLevel());
        assertFalse(response.isPriorityBookingEligible());
        assertFalse(response.isPeakRestricted());
    }

    @ParameterizedTest(name = "Fairness ratio {0} maps to tier {1}")
    @CsvSource({
            "1.00, FAIR",
            "0.90, FAIR",
            "1.10, FAIR",
            "0.89, SLIGHTLY_IMBALANCED",
            "0.75, SLIGHTLY_IMBALANCED",
            "1.11, SLIGHTLY_IMBALANCED",
            "1.25, SLIGHTLY_IMBALANCED",
            "0.74, IMBALANCED",
            "0.50, IMBALANCED",
            "1.26, IMBALANCED",
            "1.50, IMBALANCED",
            "0.49, SEVERELY_IMBALANCED",
            "0.10, SEVERELY_IMBALANCED",
            "1.51, SEVERELY_IMBALANCED",
            "2.50, SEVERELY_IMBALANCED"
    })
    @DisplayName("11. Parameterized Imbalance Classification Tiers: Exact boundary validation per BR-FAIR-02")
    void testDetermineImbalanceLevel(String ratioStr, ImbalanceLevel expectedLevel) {
        BigDecimal ratio = new BigDecimal(ratioStr);
        assertEquals(expectedLevel, fairUsageService.determineImbalanceLevel(ratio));
    }

    @Test
    @DisplayName("12. Score Boundedness: Score is never negative, even for extreme over-users")
    void testScoreBoundedness() {
        BigDecimal extremeRatio = new BigDecimal("3.50");
        BigDecimal score = fairUsageService.calculateFairnessScore(extremeRatio);
        assertEquals(new BigDecimal("0.00"), score, "Score must be bounded at 0.00 minimum");
    }
}
