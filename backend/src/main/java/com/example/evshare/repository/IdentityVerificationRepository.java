package com.example.evshare.repository;

import com.example.evshare.entity.IdentityVerification;
import com.example.evshare.entity.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {

    Optional<IdentityVerification> findByUserId(Long userId);

    Optional<IdentityVerification> findByIdCardNumber(String idCardNumber);

    List<IdentityVerification> findByVerificationStatus(VerificationStatus status);
}
