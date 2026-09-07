package com.example.evshare.repository;

import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.VehicleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVin(String vin);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    List<Vehicle> findByStatus(VehicleStatus status);

    Page<Vehicle> findByStatus(VehicleStatus status, Pageable pageable);

    Page<Vehicle> findByManufacturerIgnoreCase(String manufacturer, Pageable pageable);

    Page<Vehicle> findByStatusAndManufacturerIgnoreCase(VehicleStatus status, String manufacturer, Pageable pageable);

    List<Vehicle> findByStallLocationCode(String stallLocationCode);

    boolean existsByVin(String vin);

    boolean existsByLicensePlate(String licensePlate);

    long countByStatus(VehicleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);
}
