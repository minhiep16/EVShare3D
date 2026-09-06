package com.example.evshare.repository;

import com.example.evshare.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {

    List<AiRecommendation> findByGroupId(Long groupId);

    List<AiRecommendation> findByGroupIdAndIsAcknowledged(Long groupId, Boolean isAcknowledged);

    List<AiRecommendation> findByTargetUserId(Long targetUserId);
}
