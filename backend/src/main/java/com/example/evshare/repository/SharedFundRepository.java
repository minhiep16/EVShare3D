package com.example.evshare.repository;

import com.example.evshare.entity.SharedFund;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedFundRepository extends JpaRepository<SharedFund, Long> {

    Optional<SharedFund> findByGroupId(Long groupId);

    boolean existsByGroupId(Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sf FROM SharedFund sf WHERE sf.id = :id")
    Optional<SharedFund> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sf FROM SharedFund sf WHERE sf.group.id = :groupId")
    Optional<SharedFund> findByGroupIdWithLock(@Param("groupId") Long groupId);
}
