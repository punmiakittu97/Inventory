package com.example.incidentintake;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies the real property-injection wiring: with the placeholder API key in effect
 * (the same default application.properties falls back to when ANTHROPIC_API_KEY is unset),
 * POST /chat must fail fast with a clear, actionable error rather than reaching Anthropic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.ai.anthropic.api-key=REPLACE_WITH_YOUR_ANTHROPIC_API_KEY")
class ChatEndpointIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void chat_withPlaceholderApiKey_returns503WithActionableMessage() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message", containsString("spring.ai.anthropic.api-key")))
                .andExpect(jsonPath("$.message", containsString("ANTHROPIC_API_KEY")))
                .andExpect(jsonPath("$.message", containsString("restart")));
    }
}
