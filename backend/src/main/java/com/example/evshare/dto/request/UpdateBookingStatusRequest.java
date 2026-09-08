package com.example.evshare.dto.request;

import com.example.evshare.entity.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateBookingStatusRequest {

    @NotNull(message = "Booking status cannot be null")
    @JsonAlias({"targetStatus", "status"})
    private BookingStatus status;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;

    public UpdateBookingStatusRequest() {
    }

    public UpdateBookingStatusRequest(BookingStatus status) {
        this.status = status;
    }

    public UpdateBookingStatusRequest(BookingStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BookingStatus getTargetStatus() {
        return status;
    }

    public void setTargetStatus(BookingStatus targetStatus) {
        this.status = targetStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
