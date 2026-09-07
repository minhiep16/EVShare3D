package com.example.evshare.repository;

import com.example.evshare.entity.CoOwnershipContract;
import com.example.evshare.entity.enums.ContractStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoOwnershipContractRepository extends JpaRepository<CoOwnershipContract, Long> {

    List<CoOwnershipContract> findByGroupId(Long groupId);

    Optional<CoOwnershipContract> findByGroupIdAndStatus(Long groupId, ContractStatus status);

    List<CoOwnershipContract> findByGroupIdOrderByVersionDesc(Long groupId);

    Optional<CoOwnershipContract> findTopByGroupIdOrderByVersionDesc(Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CoOwnershipContract c WHERE c.id = :id")
    Optional<CoOwnershipContract> findByIdForUpdate(@Param("id") Long id);
}
