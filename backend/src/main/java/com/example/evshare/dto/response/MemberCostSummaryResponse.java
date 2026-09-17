package com.example.evshare.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Itemized syndicate co-owner cost liability summary")
public class MemberCostSummaryResponse {

    private Long userId;
    private String userName;
    private BigDecimal equityPercentage;
    private BigDecimal usagePercentage;
    private BigDecimal allocatedTotalVnd;
    private BigDecimal paidTotalVnd;
    private BigDecimal outstandingDueVnd;
    private String settlementStatus; // SETTLED, PARTIAL, OVERDUE

    public MemberCostSummaryResponse() {
    }

    public MemberCostSummaryResponse(Long userId, String userName, BigDecimal equityPercentage,
                                     BigDecimal usagePercentage, BigDecimal allocatedTotalVnd,
                                     BigDecimal paidTotalVnd, BigDecimal outstandingDueVnd,
                                     String settlementStatus) {
        this.userId = userId;
        this.userName = userName;
        this.equityPercentage = equityPercentage;
        this.usagePercentage = usagePercentage;
        this.allocatedTotalVnd = allocatedTotalVnd;
        this.paidTotalVnd = paidTotalVnd;
        this.outstandingDueVnd = outstandingDueVnd;
        this.settlementStatus = settlementStatus;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getEquityPercentage() {
        return equityPercentage;
    }

    public void setEquityPercentage(BigDecimal equityPercentage) {
        this.equityPercentage = equityPercentage;
    }

    public BigDecimal getUsagePercentage() {
        return usagePercentage;
    }

    public void setUsagePercentage(BigDecimal usagePercentage) {
        this.usagePercentage = usagePercentage;
    }

    public BigDecimal getAllocatedTotalVnd() {
        return allocatedTotalVnd;
    }

    public void setAllocatedTotalVnd(BigDecimal allocatedTotalVnd) {
        this.allocatedTotalVnd = allocatedTotalVnd;
    }

    public BigDecimal getPaidTotalVnd() {
        return paidTotalVnd;
    }

    public void setPaidTotalVnd(BigDecimal paidTotalVnd) {
        this.paidTotalVnd = paidTotalVnd;
    }

    public BigDecimal getOutstandingDueVnd() {
        return outstandingDueVnd;
    }

    public void setOutstandingDueVnd(BigDecimal outstandingDueVnd) {
        this.outstandingDueVnd = outstandingDueVnd;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }
}
