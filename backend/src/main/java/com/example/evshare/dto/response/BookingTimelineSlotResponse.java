package com.example.evshare.dto.response;

import com.example.evshare.entity.Booking;
import com.example.evshare.entity.enums.BookingStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class BookingTimelineSlotResponse {

    private Long bookingId;
    private Long userId;
    private Instant startTime;
    private Instant endTime;
    private Instant bufferedEndTime;
    private BookingStatus status;
    private Boolean isMyBooking;

    public BookingTimelineSlotResponse() {
    }

    public BookingTimelineSlotResponse(Long bookingId, Long userId, Instant startTime, Instant endTime,
                                       Instant bufferedEndTime, BookingStatus status, Boolean isMyBooking) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bufferedEndTime = bufferedEndTime;
        this.status = status;
        this.isMyBooking = isMyBooking;
    }

    public static BookingTimelineSlotResponse fromEntity(Booking booking, Long currentUserId) {
        if (booking == null) {
            return null;
        }
        Instant bufferedEnd = booking.getEndTime() != null
                ? booking.getEndTime().plus(30, ChronoUnit.MINUTES)
                : null;
        Boolean myBooking = currentUserId != null && booking.getUser() != null
                && currentUserId.equals(booking.getUser().getId());

        return new BookingTimelineSlotResponse(
                booking.getId(),
                booking.getUser() != null ? booking.getUser().getId() : null,
                booking.getStartTime(),
                booking.getEndTime(),
                bufferedEnd,
                booking.getStatus(),
                myBooking
        );
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getBufferedEndTime() {
        return bufferedEndTime;
    }

    public void setBufferedEndTime(Instant bufferedEndTime) {
        this.bufferedEndTime = bufferedEndTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Boolean getIsMyBooking() {
        return isMyBooking;
    }

    public void setIsMyBooking(Boolean myBooking) {
        isMyBooking = myBooking;
    }
}
