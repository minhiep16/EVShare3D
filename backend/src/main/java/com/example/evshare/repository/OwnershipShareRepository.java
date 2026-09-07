package com.example.evshare.repository;

import com.example.evshare.entity.OwnershipShare;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OwnershipShareRepository extends JpaRepository<OwnershipShare, Long> {

    List<OwnershipShare> findByGroupId(Long groupId);

    List<OwnershipShare> findByGroupIdAndIsActiveTrue(Long groupId);

    long countByGroupIdAndIsActiveTrue(Long groupId);

    List<OwnershipShare> findByUserId(Long userId);

    Optional<OwnershipShare> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<OwnershipShare> findByShareCertificateNumber(String shareCertificateNumber);

    @Query("SELECT COALESCE(SUM(s.percentage), 0) FROM OwnershipShare s WHERE s.group.id = :groupId AND s.isActive = true")
    BigDecimal sumActivePercentagesByGroupId(@Param("groupId") Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM OwnershipShare s WHERE s.id = :id")
    Optional<OwnershipShare> findByIdForUpdate(@Param("id") Long id);
}
