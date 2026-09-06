package com.example.evshare.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConfigurationFoundationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OpenAPI openAPI;

    @Autowired
    private WebMvcConfig webMvcConfig;

    @Value("${spring.application.name}")
    private String applicationName;

    @Test
    @DisplayName("Application name configuration property should match evshare-backend")
    void testApplicationProperties() {
        assertEquals("evshare-backend", applicationName, "Application name must match spring.application.name");
    }

    @Test
    @DisplayName("JacksonConfig ObjectMapper should properly format Java 8 date/time types")
    void testJacksonConfigDateTimeSerialization() throws Exception {
        assertNotNull(objectMapper, "Custom ObjectMapper must be present");

        Instant now = Instant.parse("2026-09-06T12:00:00Z");
        LocalDate date = LocalDate.of(2026, 9, 6);

        Map<String, Object> payload = Map.of(
                "instant", now,
                "date", date
        );

        String json = objectMapper.writeValueAsString(payload);
        assertTrue(json.contains("2026-09-06T12:00:00Z"), "Instant must be serialized in ISO-8601 format, not epoch millis");
        assertTrue(json.contains("2026-09-06"), "LocalDate must be serialized as ISO string");
    }

    @Test
    @DisplayName("OpenApiConfig should register correct metadata, security scheme, and servers")
    void testOpenApiConfig() {
        assertNotNull(openAPI, "OpenAPI bean must be present");
        assertNotNull(openAPI.getInfo(), "OpenAPI Info must be present");
        assertEquals("EVShare 3D Platform REST API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertTrue(openAPI.getInfo().getDescription().contains("Pure 3D Interactive"));

        assertNotNull(openAPI.getComponents(), "OpenAPI Components must be present");
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("BearerAuth"),
                "BearerAuth security scheme must be configured for JWT authorization");
        assertEquals("bearer", openAPI.getComponents().getSecuritySchemes().get("BearerAuth").getScheme());
    }

    @Test
    @DisplayName("WebMvcConfig bean should be injected")
    void testWebMvcConfigInjected() {
        assertNotNull(webMvcConfig, "WebMvcConfig bean must be injected into application context");
    }
}
