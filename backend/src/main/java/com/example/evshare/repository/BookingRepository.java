package com.example.evshare.repository;

import com.example.evshare.entity.Booking;
import com.example.evshare.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    List<Booking> findByVehicleId(Long vehicleId);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);

    List<Booking> findByVehicleIdOrderByStartTimeDesc(Long vehicleId);

    List<Booking> findByVehicleIdAndStatus(Long vehicleId, BookingStatus status);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    Page<Booking> findByVehicleId(Long vehicleId, Pageable pageable);

    Page<Booking> findByVehicleIdAndStatus(Long vehicleId, BookingStatus status, Pageable pageable);

    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    long countByVehicleIdAndStatus(Long vehicleId, BookingStatus status);

    long countByUserIdAndStatus(Long userId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
           "AND b.status NOT IN (com.example.evshare.entity.enums.BookingStatus.CANCELLED, com.example.evshare.entity.enums.BookingStatus.REJECTED) " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findOverlappingBookings(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
           "AND b.status NOT IN (com.example.evshare.entity.enums.BookingStatus.CANCELLED, com.example.evshare.entity.enums.BookingStatus.REJECTED) " +
           "AND b.startTime < :bufferedEnd AND b.endTime > :bufferedStart")
    List<Booking> findOverlappingBookingsWithBuffer(
            @Param("vehicleId") Long vehicleId,
            @Param("bufferedStart") Instant bufferedStart,
            @Param("bufferedEnd") Instant bufferedEnd
    );

    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
           "AND b.id != :excludeBookingId " +
           "AND b.status NOT IN (com.example.evshare.entity.enums.BookingStatus.CANCELLED, com.example.evshare.entity.enums.BookingStatus.REJECTED) " +
           "AND b.startTime < :bufferedEnd AND b.endTime > :bufferedStart")
    List<Booking> findOverlappingBookingsWithBufferExcludingId(
            @Param("vehicleId") Long vehicleId,
            @Param("excludeBookingId") Long excludeBookingId,
            @Param("bufferedStart") Instant bufferedStart,
            @Param("bufferedEnd") Instant bufferedEnd
    );

    @Query("SELECT b FROM Booking b WHERE b.vehicle.id = :vehicleId " +
           "AND b.status NOT IN (com.example.evshare.entity.enums.BookingStatus.CANCELLED, com.example.evshare.entity.enums.BookingStatus.REJECTED) " +
           "AND b.startTime < :to AND b.endTime > :from " +
           "ORDER BY b.startTime ASC")
    List<Booking> findTimelineBookings(
            @Param("vehicleId") Long vehicleId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
