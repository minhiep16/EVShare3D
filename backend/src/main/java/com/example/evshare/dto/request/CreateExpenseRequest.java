package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Request payload to record a new vehicle or syndicate expense")
public class CreateExpenseRequest {

    @NotNull(message = "Ownership group ID is mandatory")
    @Schema(description = "ID of the syndicate ownership group", example = "1")
    private Long groupId;

    @Schema(description = "Optional vehicle ID (defaults to group's assigned vehicle if omitted)", example = "1")
    private Long vehicleId;

    @NotBlank(message = "Expense title cannot be blank")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    @Schema(description = "Descriptive title for the expense invoice or receipt", example = "50kW DC Fast Charging - Station Vincom")
    private String title;

    @NotNull(message = "Expense category is mandatory")
    @Schema(description = "Category of the expense", example = "CHARGING")
    private ExpenseCategory category;

    @NotNull(message = "Expense amount is mandatory")
    @DecimalMin(value = "0.01", message = "Expense amount must be strictly greater than 0.00")
    @Schema(description = "Total incurred expense amount", example = "250000.00")
    private BigDecimal amount;

    @Schema(description = "Currency of the expense (defaults to VND)", example = "VND", defaultValue = "VND")
    private String currency = "VND";

    @Schema(description = "Allocation strategy (defaults to OWNERSHIP_BASED)", example = "OWNERSHIP_BASED", defaultValue = "OWNERSHIP_BASED")
    private AllocationStrategy allocationStrategy = AllocationStrategy.OWNERSHIP_BASED;

    @NotNull(message = "Incurred date is mandatory")
    @Schema(description = "Date when the expense was physically incurred (cannot be in the future)", example = "2026-09-08")
    private LocalDate incurredDate;

    @Size(max = 100, message = "Invoice reference cannot exceed 100 characters")
    @Schema(description = "Invoice or receipt alphanumeric reference identifier", example = "INV-2026-09-00123")
    private String invoiceReference;

    @Size(max = 255, message = "Evidence URL cannot exceed 255 characters")
    @Schema(description = "URL pointing to the uploaded invoice image or PDF document", example = "https://storage.evshare.io/invoices/inv-00123.pdf")
    private String evidenceUrl;

    @Schema(description = "Explicit flag to require receipt evidence", example = "false")
    private Boolean evidenceRequired;

    public CreateExpenseRequest() {
    }

    public CreateExpenseRequest(Long groupId, Long vehicleId, String title, ExpenseCategory category,
                                BigDecimal amount, String currency, AllocationStrategy allocationStrategy,
                                LocalDate incurredDate, String invoiceReference, String evidenceUrl, Boolean evidenceRequired) {
        this.groupId = groupId;
        this.vehicleId = vehicleId;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.currency = currency;
        this.allocationStrategy = allocationStrategy;
        this.incurredDate = incurredDate;
        this.invoiceReference = invoiceReference;
        this.evidenceUrl = evidenceUrl;
        this.evidenceRequired = evidenceRequired;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public AllocationStrategy getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(AllocationStrategy allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public LocalDate getIncurredDate() {
        return incurredDate;
    }

    public void setIncurredDate(LocalDate incurredDate) {
        this.incurredDate = incurredDate;
    }

    public String getInvoiceReference() {
        return invoiceReference;
    }

    public void setInvoiceReference(String invoiceReference) {
        this.invoiceReference = invoiceReference;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public Boolean getEvidenceRequired() {
        return evidenceRequired;
    }

    public void setEvidenceRequired(Boolean evidenceRequired) {
        this.evidenceRequired = evidenceRequired;
    }
}
