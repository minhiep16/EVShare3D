package com.example.evshare.dto.response;

import com.example.evshare.entity.Expense;
import com.example.evshare.entity.enums.AllocationStrategy;
import com.example.evshare.entity.enums.ExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "Detailed representation of a syndicate vehicle expense")
public class ExpenseResponse {

    @Schema(description = "Expense ID", example = "10")
    private Long id;

    @Schema(description = "Syndicate ownership group ID", example = "1")
    private Long groupId;

    @Schema(description = "Syndicate ownership group name", example = "VinFast VF8 Syndicate Alpha")
    private String groupName;

    @Schema(description = "Vehicle ID", example = "2")
    private Long vehicleId;

    @Schema(description = "Vehicle VIN", example = "VF8-VN-2026-0001")
    private String vehicleVin;

    @Schema(description = "Vehicle License Plate", example = "51K-999.88")
    private String vehiclePlate;

    @Schema(description = "Vehicle Model Name", example = "VinFast VF8 Plus")
    private String vehicleModel;

    @Schema(description = "Expense title", example = "50kW DC Fast Charging - Station Vincom")
    private String title;

    @Schema(description = "Expense category", example = "CHARGING")
    private ExpenseCategory category;

    @Schema(description = "Total expense amount", example = "250000.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "VND")
    private String currency;

    @Schema(description = "Allocation strategy", example = "OWNERSHIP_BASED")
    private AllocationStrategy allocationStrategy;

    @Schema(description = "Invoice reference number", example = "INV-2026-09-00123")
    private String invoiceReference;

    @Schema(description = "Evidence document or receipt URL", example = "https://storage.evshare.io/invoices/inv-00123.pdf")
    private String evidenceUrl;

    @Schema(description = "Whether evidence has been provided", example = "true")
    private Boolean evidenceProvided;

    @Schema(description = "User ID who recorded the expense", example = "3")
    private Long loggedByUserId;

    @Schema(description = "User name who recorded the expense", example = "Field Tech Tran")
    private String loggedByUserName;

    @Schema(description = "Date when expense was incurred", example = "2026-09-08")
    private LocalDate incurredDate;

    @Schema(description = "Timestamp of creation in ledger", example = "2026-09-08T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "Individual co-owner allocation splits")
    private List<ExpenseAllocationResponse> allocations = new ArrayList<>();

    public ExpenseResponse() {
    }

    public static ExpenseResponse fromEntity(Expense expense) {
        if (expense == null) {
            return null;
        }

        ExpenseResponse resp = new ExpenseResponse();
        resp.setId(expense.getId());
        if (expense.getGroup() != null) {
            resp.setGroupId(expense.getGroup().getId());
            resp.setGroupName(expense.getGroup().getGroupName());
        }
        if (expense.getVehicle() != null) {
            resp.setVehicleId(expense.getVehicle().getId());
            resp.setVehicleVin(expense.getVehicle().getVin());
            resp.setVehiclePlate(expense.getVehicle().getLicensePlate());
            resp.setVehicleModel(expense.getVehicle().getModelName());
        } else if (expense.getGroup() != null && expense.getGroup().getVehicle() != null) {
            resp.setVehicleId(expense.getGroup().getVehicle().getId());
            resp.setVehicleVin(expense.getGroup().getVehicle().getVin());
            resp.setVehiclePlate(expense.getGroup().getVehicle().getLicensePlate());
            resp.setVehicleModel(expense.getGroup().getVehicle().getModelName());
        }
        resp.setTitle(expense.getTitle());
        resp.setCategory(expense.getCategory());
        resp.setAmount(expense.getTotalAmount());
        resp.setCurrency(expense.getCurrency());
        resp.setAllocationStrategy(expense.getAllocationStrategy());
        resp.setInvoiceReference(expense.getInvoiceReference());
        resp.setEvidenceUrl(expense.getEvidenceUrl());
        resp.setEvidenceProvided(expense.isEvidenceProvided());

        if (expense.getLoggedByUser() != null) {
            resp.setLoggedByUserId(expense.getLoggedByUser().getId());
            resp.setLoggedByUserName(expense.getLoggedByUser().getFullName());
        }
        resp.setIncurredDate(expense.getIncurredDate());
        resp.setCreatedAt(expense.getCreatedAt());

        if (expense.getAllocations() != null) {
            resp.setAllocations(expense.getAllocations().stream()
                    .map(alloc -> new ExpenseAllocationResponse(
                            alloc.getId(),
                            expense.getId(),
                            alloc.getUser() != null ? alloc.getUser().getId() : null,
                            alloc.getUser() != null ? alloc.getUser().getFullName() : null,
                            alloc.getUser() != null ? alloc.getUser().getEmail() : null,
                            alloc.getAllocatedAmount(),
                            alloc.getIsSettled(),
                            alloc.getSettledAt()
                    ))
                    .collect(Collectors.toList()));
        }

        return resp;
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

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleVin() {
        return vehicleVin;
    }

    public void setVehicleVin(String vehicleVin) {
        this.vehicleVin = vehicleVin;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
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

    public Boolean getEvidenceProvided() {
        return evidenceProvided;
    }

    public void setEvidenceProvided(Boolean evidenceProvided) {
        this.evidenceProvided = evidenceProvided;
    }

    public Long getLoggedByUserId() {
        return loggedByUserId;
    }

    public void setLoggedByUserId(Long loggedByUserId) {
        this.loggedByUserId = loggedByUserId;
    }

    public String getLoggedByUserName() {
        return loggedByUserName;
    }

    public void setLoggedByUserName(String loggedByUserName) {
        this.loggedByUserName = loggedByUserName;
    }

    public LocalDate getIncurredDate() {
        return incurredDate;
    }

    public void setIncurredDate(LocalDate incurredDate) {
        this.incurredDate = incurredDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<ExpenseAllocationResponse> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<ExpenseAllocationResponse> allocations) {
        this.allocations = allocations;
    }
}
