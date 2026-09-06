package com.example.evshare.repository;

import com.example.evshare.entity.OwnershipGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OwnershipGroupRepository extends JpaRepository<OwnershipGroup, Long> {

    Optional<OwnershipGroup> findByVehicleId(Long vehicleId);

    boolean existsByVehicleId(Long vehicleId);
}
