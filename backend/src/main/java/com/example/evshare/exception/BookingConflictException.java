package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a booking creation or modification request collides with an existing
 * reservation (including turnaround buffer) or when the vehicle is in a conflicting state.
 * Handled by GlobalExceptionHandler to return HTTP 409 Conflict.
 */
public class BookingConflictException extends BusinessException {

    public BookingConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
