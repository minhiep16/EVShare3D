package com.example.evshare.repository;

import com.example.evshare.entity.Booking;
import com.example.evshare.entity.Role;
import com.example.evshare.entity.User;
import com.example.evshare.entity.Vehicle;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.entity.enums.RoleName;
import com.example.evshare.entity.enums.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("Checkpoint 05-B — Booking Model & Repository Integration Tests")
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        Role coOwnerRole = roleRepository.findByName(RoleName.ROLE_CO_OWNER)
                .orElseGet(() -> roleRepository.save(new Role(null, RoleName.ROLE_CO_OWNER)));

        testUser = new User();
        testUser.setEmail("booking.user." + UUID.randomUUID() + "@evshare.io");
        testUser.setPasswordHash("$2a$12$e8vE9N9J9g8e1m8a1k2e3h4a5s6h7f8o9r0t1e2s3t4i5n6g7");
        testUser.setFullName("Booking Test User");
        testUser.setIsActive(true);
        testUser.getRoles().add(coOwnerRole);
        testUser = userRepository.saveAndFlush(testUser);

        testVehicle = new Vehicle();
        testVehicle.setVin("VIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        testVehicle.setLicensePlate("51B-" + (int)(10000 + Math.random() * 89999));
        testVehicle.setModelName("Porsche Taycan 4S");
        testVehicle.setManufacturer("Porsche");
        testVehicle.setModel3dAssetPath("models/vehicles/taycan.glb");
        testVehicle.setStatus(VehicleStatus.AVAILABLE);
        testVehicle.setBatteryLevel(98);
        testVehicle.setOdometerKm(new BigDecimal("1500.00"));
        testVehicle.setStallLocationCode("BAY-02");
        testVehicle = vehicleRepository.saveAndFlush(testVehicle);
    }

    @Test
    @DisplayName("1. PK & Metadata: Correctly saves booking and generates ID, auditing timestamps")
    void testCreateAndPersistBooking() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant start = now.plus(2, ChronoUnit.HOURS);
        Instant end = start.plus(4, ChronoUnit.HOURS);

        Booking booking = Booking.builder()
                .vehicle(testVehicle)
                .user(testUser)
                .startTime(start)
                .endTime(end)
                .status(BookingStatus.CONFIRMED)
                .estimatedCost(new BigDecimal("450000.00"))
                .build();

        Booking saved = bookingRepository.saveAndFlush(booking);

        assertNotNull(saved.getId(), "Booking primary key ID must be generated");
        assertEquals(testVehicle.getId(), saved.getVehicle().getId(), "Vehicle FK must match target vehicle");
        assertEquals(testUser.getId(), saved.getUser().getId(), "User FK must match reserver");
        assertEquals(start, saved.getStartTime(), "Start time must match");
        assertEquals(end, saved.getEndTime(), "End time must match");
        assertEquals(BookingStatus.CONFIRMED, saved.getStatus(), "Status must be CONFIRMED");
        assertEquals(new BigDecimal("450000.00"), saved.getEstimatedCost(), "Estimated cost must match");
        assertNotNull(saved.getCreatedAt(), "Auditing createdAt must be automatically populated");
    }

    @Test
    @DisplayName("2. Default Values: Verifies status default is CONFIRMED and estimatedCost default is 0.00")
    void testBookingDefaultValues() {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(3, ChronoUnit.HOURS);

        Booking booking = new Booking();
        booking.setVehicle(testVehicle);
        booking.setUser(testUser);
        booking.setStartTime(start);
        booking.setEndTime(end);

        Booking saved = bookingRepository.saveAndFlush(booking);

        assertEquals(BookingStatus.CONFIRMED, saved.getStatus(), "Default booking status must be CONFIRMED");
        assertEquals(BigDecimal.ZERO, saved.getEstimatedCost(), "Default estimated cost must be 0.00");
        assertNotNull(saved.getCreatedAt(), "createdAt must be set");
    }

    @Test
    @DisplayName("3. All 8 Canonical Statuses: Supports PENDING, APPROVED, CONFIRMED, IN_USE, COMPLETED, CANCELLED, REJECTED, NO_SHOW")
    void testAllCanonicalStatuses() {
        BookingStatus[] statuses = {
                BookingStatus.PENDING,
                BookingStatus.APPROVED,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_USE,
                BookingStatus.COMPLETED,
                BookingStatus.CANCELLED,
                BookingStatus.REJECTED,
                BookingStatus.NO_SHOW
        };

        Instant base = Instant.now().plus(2, ChronoUnit.DAYS);

        for (int i = 0; i < statuses.length; i++) {
            BookingStatus status = statuses[i];
            Instant start = base.plus(i * 5L, ChronoUnit.HOURS);
            Instant end = start.plus(2, ChronoUnit.HOURS);

            Booking b = Booking.builder()
                    .vehicle(testVehicle)
                    .user(testUser)
                    .startTime(start)
                    .endTime(end)
                    .status(status)
                    .estimatedCost(new BigDecimal("200000.00"))
                    .build();

            Booking saved = bookingRepository.saveAndFlush(b);
            assertEquals(status, saved.getStatus(), "Status should persist as " + status);
        }
    }

    @Test
    @DisplayName("4. Vehicle FK Constraint: Rejects persistence when vehicle is null")
    void testVehicleForeignKeyNotNull() {
        Booking b = Booking.builder()
                .vehicle(null)
                .user(testUser)
                .startTime(Instant.now())
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .status(BookingStatus.CONFIRMED)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            bookingRepository.saveAndFlush(b);
        }, "Persisting booking without vehicle FK must throw DataIntegrityViolationException");
    }

    @Test
    @DisplayName("5. User FK Constraint: Rejects persistence when user is null")
    void testUserForeignKeyNotNull() {
        Booking b = Booking.builder()
                .vehicle(testVehicle)
                .user(null)
                .startTime(Instant.now())
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .status(BookingStatus.CONFIRMED)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            bookingRepository.saveAndFlush(b);
        }, "Persisting booking without user FK must throw DataIntegrityViolationException");
    }

    @Test
    @DisplayName("6. Start and End Time Constraints: Rejects persistence when start or end is null")
    void testStartEndTimeNotNull() {
        Booking b1 = Booking.builder()
                .vehicle(testVehicle)
                .user(testUser)
                .startTime(null)
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .status(BookingStatus.CONFIRMED)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            bookingRepository.saveAndFlush(b1);
        }, "Persisting booking with null start_time must throw DataIntegrityViolationException");

        Booking b2 = Booking.builder()
                .vehicle(testVehicle)
                .user(testUser)
                .startTime(Instant.now())
                .endTime(null)
                .status(BookingStatus.CONFIRMED)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            bookingRepository.saveAndFlush(b2);
        }, "Persisting booking with null end_time must throw DataIntegrityViolationException");
    }

    @Test
    @DisplayName("7. Query Methods: findByVehicleId, findByUserId, and findByVehicleIdAndStatus")
    void testQueryMethods() {
        Instant t1 = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant t2 = t1.plus(2, ChronoUnit.HOURS);
        Instant t3 = t2.plus(2, ChronoUnit.HOURS);
        Instant t4 = t3.plus(2, ChronoUnit.HOURS);

        Booking b1 = Booking.builder()
                .vehicle(testVehicle)
                .user(testUser)
                .startTime(t1)
                .endTime(t2)
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking b2 = Booking.builder()
                .vehicle(testVehicle)
                .user(testUser)
                .startTime(t3)
                .endTime(t4)
                .status(BookingStatus.COMPLETED)
                .build();

        bookingRepository.saveAndFlush(b1);
        bookingRepository.saveAndFlush(b2);

        List<Booking> vehicleBookings = bookingRepository.findByVehicleId(testVehicle.getId());
        assertTrue(vehicleBookings.size() >= 2, "Should find at least 2 bookings for test vehicle");

        List<Booking> userBookings = bookingRepository.findByUserId(testUser.getId());
        assertTrue(userBookings.size() >= 2, "Should find at least 2 bookings for test user");

        List<Booking> confirmedBookings = bookingRepository.findByVehicleIdAndStatus(testVehicle.getId(), BookingStatus.CONFIRMED);
        assertTrue(confirmedBookings.stream().anyMatch(b -> b.getId().equals(b1.getId())), "Should contain b1");

        long confirmedCount = bookingRepository.countByVehicleIdAndStatus(testVehicle.getId(), BookingStatus.CONFIRMED);
        assertTrue(confirmedCount >= 1, "Count of confirmed bookings must be >= 1");
    }

    @Test
    @DisplayName("8. Pessimistic Write Lock: findByIdForUpdate successfully acquires lock and retrieves booking")
    void testFindByIdForUpdate() {
        Instant start = Instant.now().plus(3, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        Booking b = Booking.builder()
                .vehicle(testVehicle)
                .user(testUser)
                .startTime(start)
                .endTime(end)
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking saved = bookingRepository.saveAndFlush(b);

        Optional<Booking> locked = bookingRepository.findByIdForUpdate(saved.getId());
        assertTrue(locked.isPresent(), "Locked booking must be found");
        assertEquals(saved.getId(), locked.get().getId());
    }

    @Test
    @DisplayName("9. Ordering Query: findByUserIdOrderByStartTimeDesc returns chronological descending order")
    void testOrderingQuery() {
        Instant t1 = Instant.now().plus(5, ChronoUnit.HOURS);
        Instant t2 = t1.plus(2, ChronoUnit.HOURS);
        Instant t3 = Instant.now().plus(10, ChronoUnit.HOURS);
        Instant t4 = t3.plus(2, ChronoUnit.HOURS);

        Booking earlier = bookingRepository.saveAndFlush(
                Booking.builder().vehicle(testVehicle).user(testUser).startTime(t1).endTime(t2).status(BookingStatus.CONFIRMED).build()
        );

        Booking later = bookingRepository.saveAndFlush(
                Booking.builder().vehicle(testVehicle).user(testUser).startTime(t3).endTime(t4).status(BookingStatus.CONFIRMED).build()
        );

        List<Booking> ordered = bookingRepository.findByUserIdOrderByStartTimeDesc(testUser.getId());
        assertTrue(ordered.size() >= 2);
        int laterIdx = -1;
        int earlierIdx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(later.getId())) laterIdx = i;
            if (ordered.get(i).getId().equals(earlier.getId())) earlierIdx = i;
        }
        assertTrue(laterIdx < earlierIdx, "Later booking must appear before earlier booking in descending sort");
    }
}
