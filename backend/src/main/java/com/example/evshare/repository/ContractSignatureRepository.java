package com.example.evshare.repository;

import com.example.evshare.entity.ContractSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractSignatureRepository extends JpaRepository<ContractSignature, Long> {

    List<ContractSignature> findByContractId(Long contractId);

    Optional<ContractSignature> findByContractIdAndUserId(Long contractId, Long userId);

    boolean existsByContractIdAndUserId(Long contractId, Long userId);
}
