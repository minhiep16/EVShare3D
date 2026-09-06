package com.example.evshare.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "votes",
        uniqueConstraints = @UniqueConstraint(name = "uk_proposal_user_vote", columnNames = {"proposal_id", "user_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_option_id", nullable = false)
    private VoteOption voteOption;

    @Column(name = "equity_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal equityWeight;

    @CreatedDate
    @Column(name = "voted_at", nullable = false, updatable = false)
    private Instant votedAt = Instant.now();

    public Vote() {
    }

    public Vote(Long id, Proposal proposal, User user, VoteOption voteOption, BigDecimal equityWeight, Instant votedAt) {
        this.id = id;
        this.proposal = proposal;
        this.user = user;
        this.voteOption = voteOption;
        this.equityWeight = equityWeight;
        this.votedAt = votedAt != null ? votedAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public VoteOption getVoteOption() {
        return voteOption;
    }

    public void setVoteOption(VoteOption voteOption) {
        this.voteOption = voteOption;
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
