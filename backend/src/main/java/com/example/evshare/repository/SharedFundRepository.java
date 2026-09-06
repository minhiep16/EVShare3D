package com.example.evshare.repository;

import com.example.evshare.entity.SharedFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedFundRepository extends JpaRepository<SharedFund, Long> {

    Optional<SharedFund> findByGroupId(Long groupId);

    boolean existsByGroupId(Long groupId);
}
