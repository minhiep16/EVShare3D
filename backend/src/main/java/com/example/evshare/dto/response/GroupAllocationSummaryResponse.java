package com.example.evshare.dto.response;

import com.example.evshare.entity.enums.AllocationStrategy;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authoritative syndicate group cost allocation report")
public class GroupAllocationSummaryResponse {

    private Long groupId;
    private String groupName;
    private AllocationStrategy strategy;
    private BigDecimal totalExpenseAmount;
    private BigDecimal totalAllocatedAmount;
    private Boolean isReconciled;
    private List<MemberCostSummaryResponse> memberSummaries = new ArrayList<>();

    public GroupAllocationSummaryResponse() {
    }

    public GroupAllocationSummaryResponse(Long groupId, String groupName, AllocationStrategy strategy,
                                          BigDecimal totalExpenseAmount, BigDecimal totalAllocatedAmount,
                                          Boolean isReconciled, List<MemberCostSummaryResponse> memberSummaries) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.strategy = strategy;
        this.totalExpenseAmount = totalExpenseAmount;
        this.totalAllocatedAmount = totalAllocatedAmount;
        this.isReconciled = isReconciled;
        this.memberSummaries = memberSummaries != null ? memberSummaries : new ArrayList<>();
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

    public AllocationStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(AllocationStrategy strategy) {
        this.strategy = strategy;
    }

    public BigDecimal getTotalExpenseAmount() {
        return totalExpenseAmount;
    }

    public void setTotalExpenseAmount(BigDecimal totalExpenseAmount) {
        this.totalExpenseAmount = totalExpenseAmount;
    }

    public BigDecimal getTotalAllocatedAmount() {
        return totalAllocatedAmount;
    }

    public void setTotalAllocatedAmount(BigDecimal totalAllocatedAmount) {
        this.totalAllocatedAmount = totalAllocatedAmount;
    }

    public Boolean getIsReconciled() {
        return isReconciled;
    }

    public void setIsReconciled(Boolean isReconciled) {
        this.isReconciled = isReconciled;
    }

    public List<MemberCostSummaryResponse> getMemberSummaries() {
        return memberSummaries;
    }

    public void setMemberSummaries(List<MemberCostSummaryResponse> memberSummaries) {
        this.memberSummaries = memberSummaries;
    }
}
