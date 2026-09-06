package com.example.evshare.controller;

import com.example.evshare.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "System health check and diagnostic endpoints")
public class HealthController {

    @GetMapping
    @Operation(summary = "Get system health status", description = "Returns the operational status of the EVShare 3D backend platform")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> healthInfo = new LinkedHashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("application", "EVShare 3D Platform");
        healthInfo.put("mode", "PURE_3D_METAVERSE_BACKEND");
        healthInfo.put("version", "1.0.0");
        healthInfo.put("serverTime", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.ok("EVShare 3D backend is operational", healthInfo));
    }
}
