package com.example.evshare.service;

import com.example.evshare.dto.request.CancelBookingRequest;
import com.example.evshare.dto.request.CreateBookingRequest;
import com.example.evshare.dto.request.UpdateBookingRequest;
import com.example.evshare.dto.response.BookingCancellationResponse;
import com.example.evshare.dto.response.BookingHistoryResponse;
import com.example.evshare.dto.response.BookingResponse;
import com.example.evshare.dto.response.PagedData;
import com.example.evshare.entity.enums.BookingStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingService {

    /**
     * Atomically creates a vehicle reservation enforcing business rules:
     * - Authentication & active user verification
     * - Co-owner syndicate membership verification (frontend bypass prevention)
     * - Operational vehicle status check
     * - Reservation duration bounds (30m min, 72h max, 30d advance window per BR-BKG-01)
     * - Mandatory 30-minute turnaround buffer overlap prevention per BR-BKG-02
     * - Pessimistic row locking on target vehicle
     *
     * @param request the reservation creation details
     * @param currentUserId the ID of the authenticated user
     * @param isAdmin whether the authenticated user has administrative privileges
     * @return the created booking response
     */
    BookingResponse createBooking(CreateBookingRequest request, Long currentUserId, boolean isAdmin);

    /**
     * Retrieves a booking by its primary ID, scoped to syndicate members or staff/admin.
     *
     * @param id the booking ID
     * @param currentUserId the ID of the authenticated user
     * @param isAdmin whether the authenticated user has administrative privileges
     * @return the booking response
     */
    BookingResponse getBookingById(Long id, Long currentUserId, boolean isAdmin);

    /**
     * Updates/reschedules an existing reservation:
     * - Validates caller is booking owner or admin
     * - Enforces lifecycle state (only CONFIRMED/PENDING; historical COMPLETED/CANCELLED/NO_SHOW rejected)
     * - Checks timing bounds and 30-minute buffer overlap excluding the booking itself
     * - Recalculates estimated cost
     * - Preserves audit log history
     *
     * @param id the booking ID to update
     * @param request the new reservation timing
     * @param currentUserId the ID of the authenticated user
     * @param isAdmin whether caller is platform admin
     * @return the updated booking response
     */
    BookingResponse updateBooking(Long id, UpdateBookingRequest request, Long currentUserId, boolean isAdmin);

    /**
     * Cancels an existing reservation per BR-BKG-03:
     * - Validates caller is booking owner or admin
     * - Enforces lifecycle state (rejects COMPLETED, IN_USE, already CANCELLED, NO_SHOW)
     * - If >= 12h before start: Free cancellation (0.00 fee, penalty false)
     * - If < 12h before start: Late cancellation (20% reservation fee deduction, penalty true)
     * - Transitions status to CANCELLED
     * - Preserves audit log history
     *
     * @param id the booking ID to cancel
     * @param request optional cancellation request with reason
     * @param currentUserId the ID of the authenticated user
     * @param isAdmin whether caller is platform admin
     * @return the booking cancellation response
     */
    BookingCancellationResponse cancelBooking(Long id, CancelBookingRequest request, Long currentUserId, boolean isAdmin);

    /**
     * Retrieves immutable audit trail for the reservation.
     *
     * @param id the booking ID
     * @param currentUserId the ID of the authenticated user
     * @param isAdmin whether caller is platform admin
     * @return list of chronological booking audit log entries
     */
    List<BookingHistoryResponse> getBookingHistory(Long id, Long currentUserId, boolean isAdmin);

    /**
     * Retrieves paginated booking history for the authenticated user.
     *
     * @param userId the user ID
     * @param status optional status filter
     * @param pageable pagination parameters
     * @return paged list of user bookings
     */
    PagedData<BookingResponse> getMyBookings(Long userId, BookingStatus status, Pageable pageable);

    /**
     * Executes a controlled booking lifecycle transition governed by BookingStateMachine.
     *
     * @param id the booking ID
     * @param request the target status and optional reason
     * @param currentUserId the ID of the authenticated user
     * @param isAdmin whether caller is platform admin or staff
     * @return updated booking response
     */
    BookingResponse transitionBookingStatus(Long id, com.example.evshare.dto.request.UpdateBookingStatusRequest request, Long currentUserId, boolean isAdmin);

    /**
     * Retrieves paginated booking history for a specific vehicle, scoped to syndicate members.
     *
     * @param vehicleId the vehicle ID
     * @param status optional status filter
     * @param currentUserId the ID of the authenticated user
     * @param isAdmin whether caller is staff or admin
     * @param pageable pagination parameters
     * @return paged list of vehicle bookings
     */
    PagedData<BookingResponse> getVehicleBookings(Long vehicleId, BookingStatus status, Long currentUserId, boolean isAdmin, Pageable pageable);
}
