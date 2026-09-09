package com.example.evshare.repository;

import com.example.evshare.entity.FundTransaction;
import com.example.evshare.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundTransactionRepository extends JpaRepository<FundTransaction, Long> {

    List<FundTransaction> findByFundId(Long fundId);

    List<FundTransaction> findByFundIdOrderByCreatedAtDesc(Long fundId);

    List<FundTransaction> findByFundIdOrderByCreatedAtAscIdAsc(Long fundId);

    List<FundTransaction> findByFundIdOrderByIdDesc(Long fundId);

    List<FundTransaction> findByFundIdAndTransactionType(Long fundId, TransactionType transactionType);

    Optional<FundTransaction> findByTransactionReference(String transactionReference);

    boolean existsByTransactionReference(String transactionReference);
}
