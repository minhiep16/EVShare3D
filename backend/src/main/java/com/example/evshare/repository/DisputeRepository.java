package com.example.evshare.repository;

import com.example.evshare.entity.Dispute;
import com.example.evshare.entity.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    List<Dispute> findByGroupId(Long groupId);

    List<Dispute> findByGroupIdAndStatus(Long groupId, DisputeStatus status);

    List<Dispute> findByComplainantUserId(Long complainantUserId);

    List<Dispute> findByUsageSessionId(Long usageSessionId);
}
