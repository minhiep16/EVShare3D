package com.example.evshare.controller;

import com.example.evshare.dto.request.InitiatePaymentRequest;
import com.example.evshare.dto.response.ApiResponse;
import com.example.evshare.dto.response.PaymentResponse;
import com.example.evshare.security.UserPrincipal;
import com.example.evshare.service.payment.PaymentService;
import com.example.evshare.service.payment.model.PaymentVerificationCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Finance & Payments", description = "Endpoints for syndicate payment initiation, VietQR settlement, verification, and audit tracking")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'CO_OWNER')")
    @Operation(summary = "Initiate an idempotent payment for shared fund or expense allocation",
            description = "Creates or retrieves existing payment session supporting VietQR, E-Wallet, Card, or Mock provider")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyHeader,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String idempotencyKey = idempotencyHeader != null ? idempotencyHeader : request.getIdempotencyKey();
        PaymentResponse response = paymentService.initiatePayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payment initiated successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'CO_OWNER')")
    @Operation(summary = "Retrieve payment status by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable Long id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Payment retrieved successfully", response));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'CO_OWNER')")
    @Operation(summary = "Retrieve payment status by transaction reference")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByReference(@PathVariable String reference) {
        PaymentResponse response = paymentService.getPaymentByReference(reference);
        return ResponseEntity.ok(ApiResponse.ok("Payment retrieved successfully", response));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'CO_OWNER')")
    @Operation(summary = "Verify settlement status of a pending payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable Long id,
            @RequestBody(required = false) PaymentVerificationCommand command,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey
    ) {
        PaymentResponse existing = paymentService.getPaymentById(id);
        PaymentVerificationCommand verificationCommand = command != null ? command :
                PaymentVerificationCommand.builder()
                        .transactionReference(existing.getTransactionReference())
                        .expectedAmount(existing.getAmount())
                        .build();
        PaymentResponse response = paymentService.verifyPayment(verificationCommand, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Payment verification executed", response));
    }
}
