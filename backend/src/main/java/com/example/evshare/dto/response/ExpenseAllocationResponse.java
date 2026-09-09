package com.example.evshare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Individual co-owner expense allocation liability breakdown")
public class ExpenseAllocationResponse {

    @Schema(description = "Allocation ID", example = "1")
    private Long id;

    @Schema(description = "Parent expense ID", example = "42")
    private Long expenseId;

    @Schema(description = "Co-owner user ID", example = "5")
    private Long userId;

    @Schema(description = "Co-owner full name", example = "Nguyen Van A")
    private String userName;

    @Schema(description = "Co-owner email", example = "nguyen.a@evshare.io")
    private String userEmail;

    @Schema(description = "Allocated amount due", example = "62500.00")
    private BigDecimal allocatedAmount;

    @Schema(description = "Whether this allocation has been settled", example = "false")
    private Boolean isSettled;

    @Schema(description = "Timestamp when this allocation was settled", example = "2026-09-08T15:30:00Z")
    private Instant settledAt;

    public ExpenseAllocationResponse() {
    }

    public ExpenseAllocationResponse(Long id, Long expenseId, Long userId, String userName, String userEmail,
                                   BigDecimal allocatedAmount, Boolean isSettled, Instant settledAt) {
        this.id = id;
        this.expenseId = expenseId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.allocatedAmount = allocatedAmount;
        this.isSettled = isSettled;
        this.settledAt = settledAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(Long expenseId) {
        this.expenseId = expenseId;
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

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public Boolean getIsSettled() {
        return isSettled;
    }

    public void setIsSettled(Boolean isSettled) {
        this.isSettled = isSettled;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Instant settledAt) {
        this.settledAt = settledAt;
    }
}
