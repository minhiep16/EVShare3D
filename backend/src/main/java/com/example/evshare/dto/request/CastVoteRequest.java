package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.VoteOptionKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class CastVoteRequest {

    @NotNull(message = "Vote choice is required (APPROVE, REJECT, ABSTAIN)")
    @Schema(description = "Decision chamber vote ballot choice", example = "APPROVE", allowableValues = {"APPROVE", "REJECT", "ABSTAIN"})
    private VoteOptionKey optionKey;

    public CastVoteRequest() {}

    public CastVoteRequest(VoteOptionKey optionKey) {
        this.optionKey = optionKey;
    }

    public VoteOptionKey getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(VoteOptionKey optionKey) {
        this.optionKey = optionKey;
    }
}
