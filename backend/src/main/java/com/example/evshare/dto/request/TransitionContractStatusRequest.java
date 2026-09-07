package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;

public class TransitionContractStatusRequest {

    @NotNull(message = "Target status cannot be null")
    private ContractStatus targetStatus;

    private String reason;

    public TransitionContractStatusRequest() {
    }

    public TransitionContractStatusRequest(ContractStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public TransitionContractStatusRequest(ContractStatus targetStatus, String reason) {
        this.targetStatus = targetStatus;
        this.reason = reason;
    }

    public ContractStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(ContractStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
