package com.example.incidentintake;

import com.example.incidentintake.incident.infrastructure.IncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuditHistoryTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired IncidentRepository incidentRepository;

    @BeforeEach
    void cleanUp() {
        incidentRepository.deleteAll();
    }

    @Test
    void historyContainsAuditRowPerTransition() throws Exception {
        MvcResult created = mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Audit Test\",\"severity\":\"LOW\",\"reportedBy\":\"dev\"}"))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"changedBy\":\"dev\",\"notes\":\"taking it\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"changedBy\":\"dev\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/incidents/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].previousStatus").value("OPEN"))
                .andExpect(jsonPath("$[0].newStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[0].notes").value("taking it"))
                .andExpect(jsonPath("$[1].previousStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].newStatus").value("RESOLVED"));
    }

    @Test
    void historyIsOrderedByChangedAtAsc() throws Exception {
        MvcResult created = mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Order Test\",\"severity\":\"HIGH\",\"reportedBy\":\"ops\"}"))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        patchStatus(id, "IN_PROGRESS");
        patchStatus(id, "RESOLVED");
        patchStatus(id, "CLOSED");

        mockMvc.perform(get("/incidents/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].newStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].newStatus").value("RESOLVED"))
                .andExpect(jsonPath("$[2].newStatus").value("CLOSED"));
    }

    private void patchStatus(String id, String status) throws Exception {
        mockMvc.perform(patch("/incidents/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\",\"changedBy\":\"ops\"}"))
                .andExpect(status().isOk());
    }
}
