package com.example.evshare.repository;

import com.example.evshare.entity.Payment;
import com.example.evshare.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionReference(String transactionReference);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByFundId(Long fundId);

    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    boolean existsByTransactionReference(String transactionReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.transactionReference = :transactionReference")
    Optional<Payment> findByTransactionReferenceWithLock(@Param("transactionReference") String transactionReference);
}
