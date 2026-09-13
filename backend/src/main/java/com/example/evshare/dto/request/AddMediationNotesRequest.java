package com.example.evshare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for recording staff mediation review notes and factual observations")
public class AddMediationNotesRequest {

    @NotBlank(message = "Mediation notes cannot be blank")
    @Size(max = 5000, message = "Mediation notes cannot exceed 5000 characters")
    @Schema(description = "Detailed mediation notes and findings recorded by staff", example = "Review conducted with both parties. Complainant confirmed bumper dent was not present prior to check-in.")
    private String notes;

    public AddMediationNotesRequest() {
    }

    public AddMediationNotesRequest(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
