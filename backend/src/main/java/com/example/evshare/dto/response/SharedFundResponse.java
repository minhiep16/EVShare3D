package com.example.evshare.dto.response;

import com.example.evshare.entity.SharedFund;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SharedFundResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private BigDecimal currentBalance;
    private BigDecimal minimumReserveThreshold;
    private String currency;
    private Boolean isLowLiquidity;
    private Instant updatedAt;

    public SharedFundResponse() {
    }

    public SharedFundResponse(Long id, Long groupId, String groupName, BigDecimal currentBalance,
                              BigDecimal minimumReserveThreshold, String currency,
                              Boolean isLowLiquidity, Instant updatedAt) {
        this.id = id;
        this.groupId = groupId;
        this.groupName = groupName;
        this.currentBalance = currentBalance;
        this.minimumReserveThreshold = minimumReserveThreshold;
        this.currency = currency;
        this.isLowLiquidity = isLowLiquidity;
        this.updatedAt = updatedAt;
    }

    public static SharedFundResponse fromEntity(SharedFund fund) {
        if (fund == null) {
            return null;
        }
        return new SharedFundResponse(
                fund.getId(),
                fund.getGroup() != null ? fund.getGroup().getId() : null,
                fund.getGroup() != null ? fund.getGroup().getGroupName() : null,
                fund.getCurrentBalance(),
                fund.getMinimumReserveThreshold(),
                fund.getCurrency(),
                fund.isLowLiquidity(),
                fund.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getMinimumReserveThreshold() {
        return minimumReserveThreshold;
    }

    public void setMinimumReserveThreshold(BigDecimal minimumReserveThreshold) {
        this.minimumReserveThreshold = minimumReserveThreshold;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getIsLowLiquidity() {
        return isLowLiquidity;
    }

    public void setIsLowLiquidity(Boolean isLowLiquidity) {
        this.isLowLiquidity = isLowLiquidity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
