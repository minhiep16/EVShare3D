package com.example.evshare.service;

import com.example.evshare.dto.request.FundContributionRequest;
import com.example.evshare.dto.request.FundWithdrawalRequest;
import com.example.evshare.dto.response.FundAuditLogResponse;
import com.example.evshare.dto.response.FundReconciliationResponse;
import com.example.evshare.dto.response.FundTransactionResponse;
import com.example.evshare.dto.response.SharedFundResponse;
import com.example.evshare.entity.OwnershipGroup;
import com.example.evshare.entity.SharedFund;

import java.util.List;

public interface SharedFundService {

    SharedFundResponse getFundByGroupId(Long groupId, Long currentUserId);

    SharedFundResponse getFundById(Long fundId, Long currentUserId);

    SharedFundResponse getFundBalance(Long groupId, Long currentUserId);

    FundTransactionResponse contribute(Long groupId, FundContributionRequest request, Long currentUserId, String ipAddress);

    FundTransactionResponse withdraw(Long groupId, FundWithdrawalRequest request, Long currentUserId, String ipAddress);

    List<FundTransactionResponse> getTransactionHistory(Long groupId, Long currentUserId);

    List<FundAuditLogResponse> getFundAuditHistory(Long groupId, Long currentUserId);

    FundReconciliationResponse reconcileFundBalance(Long groupId, Long currentUserId);

    FundTransactionResponse getTransactionByReference(Long groupId, String reference, Long currentUserId);

    SharedFund getOrCreateFundForGroup(OwnershipGroup group);
}
