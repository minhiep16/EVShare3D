package com.example.evshare.repository;

import com.example.evshare.entity.Payment;
import com.example.evshare.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
