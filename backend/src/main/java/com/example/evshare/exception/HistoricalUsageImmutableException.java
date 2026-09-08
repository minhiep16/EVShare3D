package com.example.evshare.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain exception thrown when an operation attempts to mutate or rewrite
 * a completed historical usage session, violating the platform immutability invariant.
 */
public class HistoricalUsageImmutableException extends BusinessException {

    private final Long sessionId;

    public HistoricalUsageImmutableException(Long sessionId) {
        super(String.format("Historical usage session [id=%d] is completed and immutable. Historical usage telemetry must not be rewritten.", sessionId), HttpStatus.CONFLICT);
        this.sessionId = sessionId;
    }

    public HistoricalUsageImmutableException(String message, Long sessionId) {
        super(message, HttpStatus.CONFLICT);
        this.sessionId = sessionId;
    }

    public Long getSessionId() {
        return sessionId;
    }
}
