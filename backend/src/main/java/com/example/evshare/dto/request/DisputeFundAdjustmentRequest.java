package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.TransactionEntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload for dispute resolution with transactional SharedFund financial adjustment")
public class DisputeFundAdjustmentRequest {

    @NotBlank(message = "Arbitration reason is required")
    @Size(min = 5, max = 5000, message = "Arbitration reason must be between 5 and 5000 characters")
    @Schema(description = "Factual justification and legal rationale for the dispute resolution and fund adjustment", example = "Inspection confirms front bumper crack happened during rental. Syndicate vault covers repair.")
    private String reason;

    @NotBlank(message = "Resolution summary is required")
    @Size(min = 5, max = 5000, message = "Resolution summary must be between 5 and 5000 characters")
    @Schema(description = "Binding resolution terms detailing the financial compensation and dispute closing terms", example = "Reimburse complainant 500,000 VND from syndicate shared fund; dispute closed as RESOLVED.")
    private String resolutionSummary;

    @NotNull(message = "Adjustment amount is required")
    @DecimalMin(value = "0.01", message = "Adjustment amount must be strictly greater than 0.00")
    @Schema(description = "Financial adjustment amount in fund currency (e.g. VND)", example = "500000.00")
    private BigDecimal amount;

    @NotNull(message = "Transaction entry type is required (DEBIT for fund payout, CREDIT for penalty deposit)")
    @Schema(description = "Entry type: DEBIT (payout deducted from SharedFund) or CREDIT (penalty deposited into SharedFund)", example = "DEBIT")
    private TransactionEntryType entryType;

    public DisputeFundAdjustmentRequest() {
    }

    public DisputeFundAdjustmentRequest(String reason, String resolutionSummary, BigDecimal amount, TransactionEntryType entryType) {
        this.reason = reason;
        this.resolutionSummary = resolutionSummary;
        this.amount = amount;
        this.entryType = entryType;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(TransactionEntryType entryType) {
        this.entryType = entryType;
    }
}
