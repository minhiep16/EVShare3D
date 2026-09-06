package com.example.evshare.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "dispute_evidences")
@EntityListeners(AuditingEntityListener.class)
public class DisputeEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedByUser;

    @Column(name = "file_url", nullable = false, length = 255)
    private String fileUrl;

    @Column(name = "mesh_3d_defect_coordinates", columnDefinition = "json")
    private String mesh3dDefectCoordinates;

    @Column(name = "description", length = 255)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DisputeEvidence() {
    }

    public DisputeEvidence(Long id, Dispute dispute, User uploadedByUser, String fileUrl, String mesh3dDefectCoordinates, String description, Instant createdAt) {
        this.id = id;
        this.dispute = dispute;
        this.uploadedByUser = uploadedByUser;
        this.fileUrl = fileUrl;
        this.mesh3dDefectCoordinates = mesh3dDefectCoordinates;
        this.description = description;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dispute getDispute() {
        return dispute;
    }

    public void setDispute(Dispute dispute) {
        this.dispute = dispute;
    }

    public User getUploadedByUser() {
        return uploadedByUser;
    }

    public void setUploadedByUser(User uploadedByUser) {
        this.uploadedByUser = uploadedByUser;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getMesh3dDefectCoordinates() {
        return mesh3dDefectCoordinates;
    }

    public void setMesh3dDefectCoordinates(String mesh3dDefectCoordinates) {
        this.mesh3dDefectCoordinates = mesh3dDefectCoordinates;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
