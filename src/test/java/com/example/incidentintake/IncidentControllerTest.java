package com.example.incidentintake;

import com.example.incidentintake.incident.infrastructure.IncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class IncidentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired IncidentRepository incidentRepository;

    @BeforeEach
    void cleanUp() {
        incidentRepository.deleteAll();
    }

    @Test
    void createIncident_returnsCreated() throws Exception {
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Disk full\",\"severity\":\"HIGH\",\"reportedBy\":\"alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createIncident_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"severity\":\"HIGH\",\"reportedBy\":\"alice\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void createIncident_idempotencyOnExternalRefId_returns200WithSameIncident() throws Exception {
        String body = "{\"title\":\"DB down\",\"severity\":\"CRITICAL\",\"reportedBy\":\"bob\",\"externalReferenceId\":\"EXT-001\"}";

        MvcResult first = mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/incidents/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listIncidents_filterBySeverity() throws Exception {
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\",\"severity\":\"LOW\",\"reportedBy\":\"x\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"B\",\"severity\":\"HIGH\",\"reportedBy\":\"x\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/incidents").param("severity", "LOW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].severity").value("LOW"));
    }

    @Test
    void listIncidents_filterByStatus() throws Exception {
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"C\",\"severity\":\"MEDIUM\",\"reportedBy\":\"y\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/incidents").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void listIncidents_filterByReportedBy() throws Exception {
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"D\",\"severity\":\"MEDIUM\",\"reportedBy\":\"charlie\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/incidents").param("reportedBy", "charlie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportedBy").value("charlie"));
    }

    @Test
    void listIncidents_filterByStatusOnHold() throws Exception {
        MvcResult created = mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"E\",\"severity\":\"HIGH\",\"reportedBy\":\"dave\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_HOLD\",\"changedBy\":\"dave\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/incidents").param("status", "ON_HOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ON_HOLD"));
    }

    @Test
    void listIncidents_combinedSeverityAndStatusFilter() throws Exception {
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"F\",\"severity\":\"CRITICAL\",\"reportedBy\":\"eve\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"G\",\"severity\":\"LOW\",\"reportedBy\":\"eve\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/incidents")
                        .param("severity", "CRITICAL")
                        .param("reportedBy", "eve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }
}
