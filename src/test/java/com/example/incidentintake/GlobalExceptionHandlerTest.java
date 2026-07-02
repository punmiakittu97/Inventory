package com.example.incidentintake;

import com.example.incidentintake.common.exception.GlobalExceptionHandler;
import com.example.incidentintake.common.exception.IncidentNotFoundException;
import com.example.incidentintake.common.exception.InvalidStatusTransitionException;
import com.example.incidentintake.incident.domain.IncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void incidentNotFound_returns404WithStructuredBody() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void invalidStatusTransition_returns422WithStructuredBody() throws Exception {
        mockMvc.perform(get("/test/invalid-transition"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"));
    }

    @Test
    void unhandledException_returns500WithStructuredBody() throws Exception {
        mockMvc.perform(get("/test/runtime-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void validation_missingRequiredField_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // Minimal controller that throws each exception type on demand
    @RestController
    @RequestMapping("/test")
    static class ThrowingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new IncidentNotFoundException(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        }

        @GetMapping("/invalid-transition")
        void invalidTransition() {
            throw new InvalidStatusTransitionException(IncidentStatus.OPEN, IncidentStatus.CLOSED);
        }

        @GetMapping("/runtime-error")
        void runtimeError() {
            throw new RuntimeException("something blew up");
        }

        @PostMapping("/validate")
        void validate(@RequestBody @jakarta.validation.Valid ValidatedBody body) {}

        record ValidatedBody(@jakarta.validation.constraints.NotBlank(message = "name must not be blank") String name) {}
    }
}
