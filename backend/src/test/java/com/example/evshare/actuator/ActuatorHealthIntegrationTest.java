package com.example.evshare.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /actuator/health should return 200 OK and status UP")
    void testActuatorHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/health/liveness should return 200 OK and status UP")
    void testActuatorLivenessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/health/readiness should return 200 OK and status UP")
    void testActuatorReadinessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/info should return 200 OK")
    void testActuatorInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    @DisplayName("GET /actuator/metrics with ADMIN role should return 200 OK")
    void testActuatorMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem("jvm.memory.used")));
    }

    @Test
    @DisplayName("GET /actuator/metrics without authentication should return 401 Unauthorized")
    void testActuatorMetricsEndpoint_Unauthorized() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/health should return 200 OK with EVShare 3D operational status")
    void testApiV1HealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("operational")))
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.data.mode", is("PURE_3D_METAVERSE_BACKEND")));
    }
}
