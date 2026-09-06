package com.example.evshare.entity;

import com.example.evshare.entity.enums.ProposalStatus;
import com.example.evshare.entity.enums.ProposalType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "proposals")
@EntityListeners(AuditingEntityListener.class)
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private OwnershipGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_user_id", nullable = false)
    private User proposerUser;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "proposal_type", nullable = false, length = 40)
    private ProposalType proposalType;

    @Column(name = "voting_deadline", nullable = false)
    private Instant votingDeadline;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private ProposalStatus status = ProposalStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Proposal() {
    }

    public Proposal(Long id, OwnershipGroup group, User proposerUser, String title, String description, ProposalType proposalType, Instant votingDeadline, ProposalStatus status, Instant createdAt) {
        this.id = id;
        this.group = group;
        this.proposerUser = proposerUser;
        this.title = title;
        this.description = description;
        this.proposalType = proposalType;
        this.votingDeadline = votingDeadline;
        this.status = status != null ? status : ProposalStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OwnershipGroup getGroup() {
        return group;
    }

    public void setGroup(OwnershipGroup group) {
        this.group = group;
    }

    public User getProposerUser() {
        return proposerUser;
    }

    public void setProposerUser(User proposerUser) {
        this.proposerUser = proposerUser;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProposalType getProposalType() {
        return proposalType;
    }

    public void setProposalType(ProposalType proposalType) {
        this.proposalType = proposalType;
    }

    public Instant getVotingDeadline() {
        return votingDeadline;
    }

    public void setVotingDeadline(Instant votingDeadline) {
        this.votingDeadline = votingDeadline;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public void setStatus(ProposalStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
