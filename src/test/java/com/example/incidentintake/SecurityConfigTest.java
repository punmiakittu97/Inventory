package com.example.incidentintake;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired MockMvc mockMvc;

    @Test
    void protectedEndpoint_withoutCredentials_returns401() throws Exception {
        mockMvc.perform(get("/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withWrongCredentials_returns401() throws Exception {
        mockMvc.perform(get("/incidents")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidCredentials_returns200() throws Exception {
        mockMvc.perform(get("/incidents")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "changeme")))
                .andExpect(status().isOk());
    }

    @Test
    void mcpToolsEndpoint_withoutCredentials_returns401() throws Exception {
        mockMvc.perform(get("/mcp/tools"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealth_withoutCredentials_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
