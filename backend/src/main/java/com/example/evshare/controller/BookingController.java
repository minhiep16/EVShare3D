package com.example.evshare.controller;

import com.example.evshare.dto.request.CancelBookingRequest;
import com.example.evshare.dto.request.CreateBookingRequest;
import com.example.evshare.dto.request.UpdateBookingRequest;
import com.example.evshare.dto.request.UpdateBookingStatusRequest;
import com.example.evshare.dto.response.*;
import com.example.evshare.entity.enums.BookingStatus;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.BookingAvailabilityService;
import com.example.evshare.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Booking & Availability", description = "Endpoints for 3D booking timeline, availability checks, and reservation scheduling")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingAvailabilityService bookingAvailabilityService;
    private final BookingService bookingService;

    public BookingController(BookingAvailabilityService bookingAvailabilityService,
                             BookingService bookingService) {
        this.bookingAvailabilityService = bookingAvailabilityService;
        this.bookingService = bookingService;
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isVehicleGroupMember(#vehicleId, principal.id))")
    @Operation(summary = "Check vehicle booking availability",
            description = "Evaluates vehicle operational status, booking constraints, and existing reservations with 30-min turnaround buffer")
    public ResponseEntity<ApiResponse<VehicleAvailabilityResponse>> checkAvailability(
            @RequestParam Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        VehicleAvailabilityResponse response = bookingAvailabilityService.checkAvailability(vehicleId, startTime, endTime, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Availability evaluated successfully", response));
    }

    @GetMapping("/timeline")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isVehicleGroupMember(#vehicleId, principal.id))")
    @Operation(summary = "Get 3D booking timeline occupancy slots",
            description = "Retrieves chronological scheduled intervals for a vehicle between 'from' and 'to' excluding cancelled/rejected slots")
    public ResponseEntity<ApiResponse<List<BookingTimelineSlotResponse>>> getTimeline(
            @RequestParam Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        List<BookingTimelineSlotResponse> response = bookingAvailabilityService.getTimeline(vehicleId, from, to, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Booking timeline retrieved successfully", response));
    }

    @PostMapping
    @PreAuthorize("hasRole('CO_OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Create vehicle reservation",
            description = "Atomically books a vehicle slot with pessimistic concurrency control, membership verification, and turnaround buffer enforcement")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long currentUserId = principal != null ? principal.getId() : null;
        BookingResponse response = bookingService.createBooking(request, currentUserId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Booking created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking by ID", description = "Retrieves reservation details")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        Long currentUserId = principal != null ? principal.getId() : null;
        BookingResponse response = bookingService.getBookingById(id, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Booking retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CO_OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Update/reschedule vehicle reservation",
            description = "Modifies reservation schedule enforcing lifecycle rules, concurrency locking, and turnaround buffer")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long currentUserId = principal != null ? principal.getId() : null;
        BookingResponse response = bookingService.updateBooking(id, request, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Booking rescheduled successfully", response));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CO_OWNER') or hasRole('ADMIN')")
    @Operation(summary = "Cancel vehicle reservation",
            description = "Cancels booking applying BR-BKG-03 penalty rules (free cancellation >=12h, 20% penalty fee <12h)")
    public ResponseEntity<ApiResponse<BookingCancellationResponse>> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) CancelBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long currentUserId = principal != null ? principal.getId() : null;
        BookingCancellationResponse response = bookingService.cancelBooking(id, request, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(response.getMessage(), response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CO_OWNER', 'STAFF', 'ADMIN')")
    @Operation(summary = "Transition booking lifecycle status",
            description = "Executes controlled state transition according to BookingStateMachine")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        Long currentUserId = principal != null ? principal.getId() : null;
        BookingResponse response = bookingService.transitionBookingStatus(id, request, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Booking status updated successfully", response));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get booking audit history",
            description = "Retrieves immutable audit trail entries recorded for this reservation")
    public ResponseEntity<ApiResponse<List<BookingHistoryResponse>>> getBookingHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        Long currentUserId = principal != null ? principal.getId() : null;
        List<BookingHistoryResponse> history = bookingService.getBookingHistory(id, currentUserId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Booking audit history retrieved successfully", history));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's booking history",
            description = "Retrieves paginated reservations for the authenticated co-owner")
    public ResponseEntity<ApiResponse<PagedData<BookingResponse>>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        PagedData<BookingResponse> response = bookingService.getMyBookings(currentUserId, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("User reservations retrieved successfully", response));
    }

    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or (hasRole('CO_OWNER') and @ownershipSecurity.isVehicleGroupMember(#vehicleId, principal.id))")
    @Operation(summary = "Get vehicle booking history",
            description = "Retrieves paginated reservations for a syndicate vehicle")
    public ResponseEntity<ApiResponse<PagedData<BookingResponse>>> getVehicleBookings(
            @PathVariable Long vehicleId,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 20, sort = "startTime", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        Long currentUserId = principal != null ? principal.getId() : null;
        PagedData<BookingResponse> response = bookingService.getVehicleBookings(vehicleId, status, currentUserId, isAdmin, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle reservations retrieved successfully", response));
    }
}
