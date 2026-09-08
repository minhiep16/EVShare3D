package com.example.evshare.dto.response;

import com.example.evshare.entity.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingCancellationResponse {

    private Long bookingId;
    private BookingStatus status;
    private BigDecimal cancellationFee;
    private Boolean isLateCancellation;
    private Boolean penaltyApplied;
    private String cancellationReason;
    private Instant cancelledAt;
    private String message;

    public BookingCancellationResponse() {
    }

    public BookingCancellationResponse(Long bookingId, BookingStatus status, BigDecimal cancellationFee,
                                       Boolean isLateCancellation, Boolean penaltyApplied,
                                       String cancellationReason, Instant cancelledAt, String message) {
        this.bookingId = bookingId;
        this.status = status;
        this.cancellationFee = cancellationFee;
        this.isLateCancellation = isLateCancellation;
        this.penaltyApplied = penaltyApplied;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
        this.message = message;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(BigDecimal cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public Boolean getIsLateCancellation() {
        return isLateCancellation;
    }

    public void setIsLateCancellation(Boolean lateCancellation) {
        isLateCancellation = lateCancellation;
    }

    public Boolean getPenaltyApplied() {
        return penaltyApplied;
    }

    public void setPenaltyApplied(Boolean penaltyApplied) {
        this.penaltyApplied = penaltyApplied;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
