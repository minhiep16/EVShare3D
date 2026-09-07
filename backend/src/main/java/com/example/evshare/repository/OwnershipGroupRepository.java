package com.example.evshare.repository;

import com.example.evshare.entity.OwnershipGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnershipGroupRepository extends JpaRepository<OwnershipGroup, Long> {

    Optional<OwnershipGroup> findByVehicleId(Long vehicleId);

    boolean existsByVehicleId(Long vehicleId);

    boolean existsByGroupName(String groupName);

    List<OwnershipGroup> findByIsActiveTrue();

    @Query("SELECT DISTINCT s.group FROM OwnershipShare s WHERE s.user.id = :userId AND s.isActive = true AND s.group.isActive = true")
    List<OwnershipGroup> findGroupsByUserId(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM OwnershipGroup g WHERE g.id = :id")
    Optional<OwnershipGroup> findByIdForUpdate(@Param("id") Long id);
}
