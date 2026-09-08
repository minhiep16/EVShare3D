package com.example.evshare.exception;

import com.example.evshare.entity.enums.BookingStatus;
import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when an attempted booking lifecycle transition violates
 * the permitted state machine rules defined in docs/BUSINESS_RULES.md.
 */
public class InvalidBookingTransitionException extends BusinessException {

    private final BookingStatus currentStatus;
    private final BookingStatus targetStatus;

    public InvalidBookingTransitionException(BookingStatus currentStatus, BookingStatus targetStatus) {
        super(String.format("Invalid booking status transition: Cannot transition booking from status '%s' to '%s'", currentStatus, targetStatus), HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public InvalidBookingTransitionException(String message, BookingStatus currentStatus, BookingStatus targetStatus) {
        super(message, HttpStatus.CONFLICT);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public BookingStatus getCurrentStatus() {
        return currentStatus;
    }

    public BookingStatus getTargetStatus() {
        return targetStatus;
    }
}
