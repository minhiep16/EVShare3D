package com.example.evshare.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "ai_recommendations")
@EntityListeners(AuditingEntityListener.class)
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private OwnershipGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Column(name = "recommendation_type", nullable = false, length = 50)
    private String recommendationType;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "suggested_actions", columnDefinition = "json")
    private String suggestedActions;

    @Column(name = "is_acknowledged", nullable = false)
    private Boolean isAcknowledged = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AiRecommendation() {
    }

    public AiRecommendation(Long id, OwnershipGroup group, User targetUser, String recommendationType, String message, String suggestedActions, Boolean isAcknowledged, Instant createdAt) {
        this.id = id;
        this.group = group;
        this.targetUser = targetUser;
        this.recommendationType = recommendationType;
        this.message = message;
        this.suggestedActions = suggestedActions;
        this.isAcknowledged = isAcknowledged != null ? isAcknowledged : false;
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

    public User getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(User targetUser) {
        this.targetUser = targetUser;
    }

    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(String suggestedActions) {
        this.suggestedActions = suggestedActions;
    }

    public Boolean getIsAcknowledged() {
        return isAcknowledged;
    }

    public void setIsAcknowledged(Boolean isAcknowledged) {
        this.isAcknowledged = isAcknowledged;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
