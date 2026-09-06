package com.example.evshare.repository;

import com.example.evshare.entity.Booking;
import com.example.evshare.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByVehicleId(Long vehicleId);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByVehicleIdAndStatus(Long vehicleId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
           "AND b.status NOT IN ('CANCELLED', 'REJECTED') " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );
}
