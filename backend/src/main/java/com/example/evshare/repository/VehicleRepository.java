package com.example.evshare.repository;

import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVin(String vin);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    List<Vehicle> findByStatus(VehicleStatus status);

    boolean existsByVin(String vin);

    boolean existsByLicensePlate(String licensePlate);
}
