package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a co-owner attempts to sponsor or initiate a governance proposal
 * without holding the required minimum active equity percentage (10.00% per BR-VOT-01).
 */
public class InsufficientEquityException extends BusinessException {

    public InsufficientEquityException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
