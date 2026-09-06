package com.example.evshare.exception;

import com.example.evshare.dto.response.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
        request.setMethod("GET");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404 status")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Vehicle not found with id 123");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Vehicle not found with id 123", response.getBody().getMessage());
        assertEquals("/api/v1/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Should handle BusinessException with custom status")
    void testHandleBusinessException() {
        BusinessException ex = new BusinessException("Total equity exceeds 100%", HttpStatus.CONFLICT);
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleBusinessException(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Total equity exceeds 100%", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle HttpMessageNotReadableException with 400 status")
    void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error",
                new MockHttpInputMessage("invalid json".getBytes(StandardCharsets.UTF_8))
        );
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Malformed JSON request body or missing required payload", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should handle HttpRequestMethodNotSupportedException with 405 status")
    void testHandleHttpRequestMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleHttpRequestMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("POST"));
    }

    @Test
    @DisplayName("Should handle MissingServletRequestParameterException with 400 status")
    void testHandleMissingServletRequestParameter() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("vin", "String");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMissingServletRequestParameter(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("vin"));
    }

    @Test
    @DisplayName("Should handle NoResourceFoundException with 404 status")
    void testHandleNoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/api/v1/unknown");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("No handler found"));
    }

    @Test
    @DisplayName("Should handle generic Exception with 500 status and safe message")
    void testHandleGeneralException() {
        Exception ex = new NullPointerException("Null pointer simulation");
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleGeneralException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("An unexpected internal error occurred. Please contact the platform administrator.", response.getBody().getMessage());
    }
}
