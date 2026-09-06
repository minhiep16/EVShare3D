package com.example.evshare.repository;

import com.example.evshare.entity.DriverLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverLicenseRepository extends JpaRepository<DriverLicense, Long> {

    Optional<DriverLicense> findByUserId(Long userId);

    Optional<DriverLicense> findByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumber(String licenseNumber);
}
