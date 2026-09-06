package com.example.evshare.repository;

import com.example.evshare.entity.DisputeEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidence, Long> {

    List<DisputeEvidence> findByDisputeId(Long disputeId);

    List<DisputeEvidence> findByUploadedByUserId(Long uploadedByUserId);
}
