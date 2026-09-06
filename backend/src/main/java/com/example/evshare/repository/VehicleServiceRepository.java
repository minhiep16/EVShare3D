package com.example.evshare.repository;

import com.example.evshare.entity.VehicleService;
import com.example.evshare.entity.enums.ServiceStatus;
import com.example.evshare.entity.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleServiceRepository extends JpaRepository<VehicleService, Long> {

    List<VehicleService> findByVehicleId(Long vehicleId);

    List<VehicleService> findByVehicleIdAndServiceStatus(Long vehicleId, ServiceStatus serviceStatus);

    List<VehicleService> findByServiceType(ServiceType serviceType);
}
