package com.example.incidentintake;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class McpToolsEndpointIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void getMcpTools_listsAllFiveRegisteredIncidentTools() throws Exception {
        mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder(
                        "create_incident", "get_incident", "list_incidents",
                        "update_incident_status", "get_incident_history")));
    }

    @Test
    void getMcpTools_eachEntryHasNameDescriptionAndSchema() throws Exception {
        mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[0].description").isNotEmpty())
                .andExpect(jsonPath("$[0].inputSchema").exists());
    }
}
