package com.example.evshare.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    @DisplayName("Should create successful ApiResponse with default message")
    void testSuccessApiResponse() {
        String data = "Hello Pure 3D World";
        ApiResponse<String> response = ApiResponse.ok(data);

        assertTrue(response.isSuccess());
        assertEquals("Operation completed successfully", response.getMessage());
        assertEquals("Hello Pure 3D World", response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @DisplayName("Should create successful ApiResponse with custom message")
    void testSuccessCustomMessage() {
        ApiResponse<Integer> response = ApiResponse.ok("Vehicle created", 42);

        assertTrue(response.isSuccess());
        assertEquals("Vehicle created", response.getMessage());
        assertEquals(42, response.getData());
    }

    @Test
    @DisplayName("Should create error ApiResponse")
    void testErrorApiResponse() {
        ApiResponse<Void> response = ApiResponse.error("Resource conflict");

        assertFalse(response.isSuccess());
        assertEquals("Resource conflict", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("Should create paged ApiResponse correctly")
    void testPagedApiResponse() {
        List<String> items = List.of("Model S", "VF 9", "Taycan");
        ApiResponse<PagedData<String>> response = ApiResponse.paged("Vehicles fetched", items, 0, 10, 25);

        assertTrue(response.isSuccess());
        assertEquals("Vehicles fetched", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(3, response.getData().getItems().size());
        assertEquals(0, response.getData().getPage());
        assertEquals(10, response.getData().getSize());
        assertEquals(25, response.getData().getTotalElements());
        assertEquals(3, response.getData().getTotalPages());
        assertTrue(response.getData().isFirst());
        assertFalse(response.getData().isLast());
        assertTrue(response.getData().isHasNext());
        assertFalse(response.getData().isHasPrevious());
    }
}
