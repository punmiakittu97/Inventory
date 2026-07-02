package com.example.incidentintake;

import com.example.incidentintake.incident.infrastructure.IncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class StatusTransitionTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired IncidentRepository incidentRepository;

    @BeforeEach
    void cleanUp() {
        incidentRepository.deleteAll();
    }

    private String createIncident() throws Exception {
        MvcResult result = mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"severity\":\"MEDIUM\",\"reportedBy\":\"tester\"}"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void validTransition_openToInProgress() throws Exception {
        String id = createIncident();
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void validTransition_inProgressToResolved() throws Exception {
        String id = createIncident();
        patchStatus(id, "IN_PROGRESS");
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
    }

    @Test
    void validTransition_resolvedToClosed() throws Exception {
        String id = createIncident();
        patchStatus(id, "IN_PROGRESS");
        patchStatus(id, "RESOLVED");
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @ParameterizedTest
    @CsvSource({"RESOLVED", "CLOSED"})
    void invalidTransition_fromOpen_returns422(String target) throws Exception {
        String id = createIncident();
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + target + "\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void invalidTransition_fromClosed_returns422() throws Exception {
        String id = createIncident();
        patchStatus(id, "IN_PROGRESS");
        patchStatus(id, "RESOLVED");
        patchStatus(id, "CLOSED");
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── ON_HOLD transitions ──────────────────────────────────────────────────

    @Test
    void validTransition_openToOnHold() throws Exception {
        String id = createIncident();
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_HOLD\",\"changedBy\":\"ops\",\"notes\":\"waiting for approval\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_HOLD"));
    }

    @Test
    void validTransition_inProgressToOnHold() throws Exception {
        String id = createIncident();
        patchStatus(id, "IN_PROGRESS");
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_HOLD\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_HOLD"));
    }

    @Test
    void validTransition_onHoldToInProgress() throws Exception {
        String id = createIncident();
        patchStatus(id, "ON_HOLD");
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void validTransition_onHoldToClosed() throws Exception {
        String id = createIncident();
        patchStatus(id, "ON_HOLD");
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @ParameterizedTest
    @CsvSource({"OPEN", "RESOLVED"})
    void invalidTransition_fromOnHold_returns422(String target) throws Exception {
        String id = createIncident();
        patchStatus(id, "ON_HOLD");
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + target + "\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    private void patchStatus(String id, String status) throws Exception {
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isOk());
    }
}
