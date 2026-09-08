package com.example.evshare.repository;

import com.example.evshare.entity.VehicleInspection;
import com.example.evshare.entity.enums.InspectionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleInspectionRepository extends JpaRepository<VehicleInspection, Long> {

    List<VehicleInspection> findByUsageSessionId(Long usageSessionId);

    List<VehicleInspection> findByUsageSessionIdOrderByCreatedAtAsc(Long usageSessionId);

    List<VehicleInspection> findByInspectorUserId(Long inspectorUserId);

    List<VehicleInspection> findByInspectionType(InspectionType inspectionType);
}
