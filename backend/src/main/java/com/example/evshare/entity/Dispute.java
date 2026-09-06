package com.example.evshare.entity;

import com.example.evshare.entity.enums.DisputeStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "disputes")
@EntityListeners(AuditingEntityListener.class)
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private OwnershipGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usage_session_id")
    private UsageSession usageSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_user_id", nullable = false)
    private User complainantUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_user_id")
    private User respondentUser;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Dispute() {
    }

    public Dispute(Long id, OwnershipGroup group, UsageSession usageSession, User complainantUser, User respondentUser, String title, String description, DisputeStatus status, String resolutionSummary, Instant createdAt) {
        this.id = id;
        this.group = group;
        this.usageSession = usageSession;
        this.complainantUser = complainantUser;
        this.respondentUser = respondentUser;
        this.title = title;
        this.description = description;
        this.status = status != null ? status : DisputeStatus.OPEN;
        this.resolutionSummary = resolutionSummary;
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

    public UsageSession getUsageSession() {
        return usageSession;
    }

    public void setUsageSession(UsageSession usageSession) {
        this.usageSession = usageSession;
    }

    public User getComplainantUser() {
        return complainantUser;
    }

    public void setComplainantUser(User complainantUser) {
        this.complainantUser = complainantUser;
    }

    public User getRespondentUser() {
        return respondentUser;
    }

    public void setRespondentUser(User respondentUser) {
        this.respondentUser = respondentUser;
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

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public void setResolutionSummary(String resolutionSummary) {
        this.resolutionSummary = resolutionSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
