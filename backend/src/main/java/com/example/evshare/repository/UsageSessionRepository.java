package com.example.evshare.repository;

import com.example.evshare.entity.UsageSession;
import com.example.evshare.entity.enums.UsageSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageSessionRepository extends JpaRepository<UsageSession, Long> {

    Optional<UsageSession> findByBookingId(Long bookingId);

    List<UsageSession> findByStatus(UsageSessionStatus status);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT s FROM UsageSession s WHERE s.booking.vehicle.id = :vehicleId")
    List<UsageSession> findByVehicleId(@Param("vehicleId") Long vehicleId);

    List<UsageSession> findByBookingUserIdOrderByCheckInTimeDesc(Long userId);

    List<UsageSession> findByBookingVehicleIdOrderByCheckInTimeDesc(Long vehicleId);

    Optional<UsageSession> findByBookingVehicleIdAndStatus(Long vehicleId, UsageSessionStatus status);
}
