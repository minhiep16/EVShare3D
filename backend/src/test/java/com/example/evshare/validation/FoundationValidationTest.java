package com.example.evshare.validation;

import com.example.evshare.controller.HealthController;
import com.example.evshare.exception.GlobalExceptionHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FoundationValidationTest.TestValidationController.class)
class FoundationValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Validator springValidator;

    private static Validator standaloneValidator;

    public static class SampleValidationDto {
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        private String username;

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be valid")
        private String email;

        @Min(value = 18, message = "Age must be at least 18")
        private Integer age;

        public SampleValidationDto() {
        }

        public SampleValidationDto(String username, String email, Integer age) {
            this.username = username;
            this.email = email;
            this.age = age;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    @RestController
    static class TestValidationController {
        @PostMapping("/api/v1/test/validate")
        public String validateInput(@jakarta.validation.Valid @RequestBody SampleValidationDto dto) {
            return "VALID";
        }
    }

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        standaloneValidator = factory.getValidator();
    }

    @Test
    @DisplayName("Jakarta Validator bean should be registered in Spring context")
    void testSpringValidatorInjected() {
        assertNotNull(springValidator, "Spring managed Validator bean must be present");
    }

    @Test
    @DisplayName("Jakarta Validation should pass for valid DTO object")
    void testValidDtoPasses() {
        SampleValidationDto dto = new SampleValidationDto("alice_ev", "alice@example.com", 25);
        Set<ConstraintViolation<SampleValidationDto>> violations = springValidator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid DTO must have zero violations");
    }

    @Test
    @DisplayName("Jakarta Validation should fail for invalid DTO with proper violation details")
    void testInvalidDtoFailsWithViolations() {
        SampleValidationDto dto = new SampleValidationDto("", "invalid-email", 16);
        Set<ConstraintViolation<SampleValidationDto>> violations = standaloneValidator.validate(dto);

        assertFalse(violations.isEmpty(), "Invalid DTO must have violations");
        assertEquals(4, violations.size(), "Should have 4 violations (2 for username: NotBlank and Size, 1 for email, 1 for age)");

        boolean hasEmailViolation = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        boolean hasAgeViolation = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("age"));
        boolean hasUsernameViolation = violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("username"));

        assertTrue(hasEmailViolation, "Email violation expected");
        assertTrue(hasAgeViolation, "Age violation expected");
        assertTrue(hasUsernameViolation, "Username violation expected");
    }

    @Test
    @DisplayName("GlobalExceptionHandler should catch validation failure and return 400 with ApiErrorResponse")
    void testValidationFailureHttpEnvelope() throws Exception {
        String invalidJson = """
                {
                    "username": "",
                    "email": "bad-email-format",
                    "age": 15
                }
                """;

        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Validation failed")))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Valid payload to validation endpoint should return 200 OK")
    void testValidationSuccessHttp() throws Exception {
        String validJson = """
                {
                    "username": "charlie_ev",
                    "email": "charlie@evshare3d.com",
                    "age": 28
                }
                """;

        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isOk())
                .andExpect(content().string("VALID"));
    }
}
