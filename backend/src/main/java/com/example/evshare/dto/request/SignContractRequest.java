package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for digital contract signing")
public class SignContractRequest {

    @Schema(description = "Explicit acknowledgment and acceptance of the contract terms", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Acknowledgment of terms is required")
    @AssertTrue(message = "You must acknowledge and accept the contract terms to sign")
    private Boolean acceptTerms;

    public SignContractRequest() {
    }

    public SignContractRequest(Boolean acceptTerms) {
        this.acceptTerms = acceptTerms;
    }

    public Boolean getAcceptTerms() {
        return acceptTerms;
    }

    public void setAcceptTerms(Boolean acceptTerms) {
        this.acceptTerms = acceptTerms;
    }
}
