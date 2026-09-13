package com.example.evshare.dto.response;

import com.example.evshare.entity.Vote;
import com.example.evshare.entity.enums.VoteOptionKey;

import java.math.BigDecimal;
import java.time.Instant;

public class VoteResponse {

    private Long id;
    private Long proposalId;
    private Long userId;
    private String voterName;
    private Long voteOptionId;
    private VoteOptionKey optionKey;
    private String optionLabel;
    private BigDecimal equityWeight;
    private Instant votedAt;

    public VoteResponse() {}

    public VoteResponse(Long id, Long proposalId, Long userId, String voterName,
                        Long voteOptionId, VoteOptionKey optionKey, String optionLabel,
                        BigDecimal equityWeight, Instant votedAt) {
        this.id = id;
        this.proposalId = proposalId;
        this.userId = userId;
        this.voterName = voterName;
        this.voteOptionId = voteOptionId;
        this.optionKey = optionKey;
        this.optionLabel = optionLabel;
        this.equityWeight = equityWeight;
        this.votedAt = votedAt;
    }

    public static VoteResponse fromEntity(Vote vote) {
        if (vote == null) {
            return null;
        }
        return new VoteResponse(
                vote.getId(),
                vote.getProposal() != null ? vote.getProposal().getId() : null,
                vote.getUser() != null ? vote.getUser().getId() : null,
                vote.getUser() != null ? vote.getUser().getFullName() : null,
                vote.getVoteOption() != null ? vote.getVoteOption().getId() : null,
                vote.getVoteOption() != null ? vote.getVoteOption().getOptionKey() : null,
                vote.getVoteOption() != null ? vote.getVoteOption().getLabel() : null,
                vote.getEquityWeight(),
                vote.getVotedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public void setProposalId(Long proposalId) {
        this.proposalId = proposalId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getVoterName() {
        return voterName;
    }

    public void setVoterName(String voterName) {
        this.voterName = voterName;
    }

    public Long getVoteOptionId() {
        return voteOptionId;
    }

    public void setVoteOptionId(Long voteOptionId) {
        this.voteOptionId = voteOptionId;
    }

    public VoteOptionKey getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(VoteOptionKey optionKey) {
        this.optionKey = optionKey;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public void setOptionLabel(String optionLabel) {
        this.optionLabel = optionLabel;
    }

    public BigDecimal getEquityWeight() {
        return equityWeight;
    }

    public void setEquityWeight(BigDecimal equityWeight) {
        this.equityWeight = equityWeight;
    }

    public Instant getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(Instant votedAt) {
        this.votedAt = votedAt;
    }
}
