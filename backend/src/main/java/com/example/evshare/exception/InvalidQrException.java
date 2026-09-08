package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when cryptographic QR code verification fails,
 * QR has expired, or domain cross-validation (vehicle, booking, time, authorization) fails.
 */
public class InvalidQrException extends BusinessException {

    public InvalidQrException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public InvalidQrException(String message, HttpStatus status) {
        super(message, status);
    }
}
