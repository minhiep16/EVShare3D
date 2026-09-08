package com.example.evshare.service;

import com.example.evshare.dto.request.QrValidationRequest;
import com.example.evshare.dto.response.QrCodeResponse;
import com.example.evshare.dto.response.QrValidationResponse;

public interface QrValidationService {

    /**
     * Generates a signed, time-bounded (5 minutes) cryptographic QR check-in token for a booking.
     * Contains only non-sensitive identifiers and tokenType 'QR_CHECK_IN'.
     *
     * @param bookingId the ID of the confirmed booking reservation
     * @param currentUserId the ID of the authenticated user requesting the QR code
     * @return QrCodeResponse containing the signed compact token and metadata
     */
    QrCodeResponse generateCheckInQr(Long bookingId, Long currentUserId);

    /**
     * Validates a physical or digital QR check-in token against cryptographic signatures,
     * expiration, and authoritative database state (vehicle, booking, time window, user authorization).
     * Never trusts QR data alone.
     *
     * @param request the QR validation request
     * @param currentUserId the ID of the authenticated user / station scanning the QR
     * @return QrValidationResponse indicating validity and verified session details
     */
    QrValidationResponse validateQr(QrValidationRequest request, Long currentUserId);
}
