package com.example.evshare.repository;

import com.example.evshare.entity.OwnershipShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnershipShareRepository extends JpaRepository<OwnershipShare, Long> {

    List<OwnershipShare> findByGroupId(Long groupId);

    List<OwnershipShare> findByUserId(Long userId);

    Optional<OwnershipShare> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    Optional<OwnershipShare> findByShareCertificateNumber(String shareCertificateNumber);
}
