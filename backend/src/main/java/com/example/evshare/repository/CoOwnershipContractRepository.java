package com.example.evshare.repository;

import com.example.evshare.entity.CoOwnershipContract;
import com.example.evshare.entity.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoOwnershipContractRepository extends JpaRepository<CoOwnershipContract, Long> {

    List<CoOwnershipContract> findByGroupId(Long groupId);

    Optional<CoOwnershipContract> findByGroupIdAndStatus(Long groupId, ContractStatus status);

    List<CoOwnershipContract> findByGroupIdOrderByVersionDesc(Long groupId);
}
