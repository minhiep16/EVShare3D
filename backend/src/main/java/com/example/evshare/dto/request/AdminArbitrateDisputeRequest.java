package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.TransactionEntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload for administrator final binding dispute arbitration")
public class AdminArbitrateDisputeRequest {

    @NotBlank(message = "Arbitration reason is required")
    @Size(min = 5, max = 5000, message = "Arbitration reason must be between 5 and 5000 characters")
    @Schema(description = "Formal justification and factual reasoning for final arbitration decision", example = "Exhaustive evidence review confirms panel damage occurred during respondent's session based on telemetry and pre/post photo metadata.")
    private String reason;

    @NotBlank(message = "Resolution summary is required")
    @Size(min = 5, max = 5000, message = "Resolution summary must be between 5 and 5000 characters")
    @Schema(description = "Binding ruling terms, ledger reconciliation adjustments, and closing resolution summary", example = "Respondent is assessed repair deductible of $350 credited to syndicate maintenance fund; dispute closed as RESOLVED.")
    private String resolutionSummary;

    @Schema(description = "Optional financial compensation/penalty adjustment amount to/from SharedFund", example = "350000.00")
    private BigDecimal fundAdjustmentAmount;

    @Schema(description = "Direction of fund adjustment: DEBIT (payout from fund to cover damages) or CREDIT (penalty/deductible deposited into fund)", example = "DEBIT")
    private TransactionEntryType fundAdjustmentType;

    public AdminArbitrateDisputeRequest() {
    }

    public AdminArbitrateDisputeRequest(String reason, String resolutionSummary) {
        this.reason = reason;
        this.resolutionSummary = resolutionSummary;
    }

    public AdminArbitrateDisputeRequest(String reason, String resolutionSummary, BigDecimal fundAdjustmentAmount, TransactionEntryType fundAdjustmentType) {
        this.reason = reason;
        this.resolutionSummary = resolutionSummary;
        this.fundAdjustmentAmount = fundAdjustmentAmount;
        this.fundAdjustmentType = fundAdjustmentType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
    }

    public BigDecimal getFundAdjustmentAmount() {
        return fundAdjustmentAmount;
    }

    public void setFundAdjustmentAmount(BigDecimal fundAdjustmentAmount) {
        this.fundAdjustmentAmount = fundAdjustmentAmount;
    }

    public TransactionEntryType getFundAdjustmentType() {
        return fundAdjustmentType;
    }

    public void setFundAdjustmentType(TransactionEntryType fundAdjustmentType) {
        this.fundAdjustmentType = fundAdjustmentType;
    }
}
