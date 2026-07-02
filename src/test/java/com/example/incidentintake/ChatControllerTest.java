package com.example.incidentintake;

import com.example.incidentintake.chat.ChatController;
import com.example.incidentintake.chat.ChatNotConfiguredException;
import com.example.incidentintake.chat.ChatRequest;
import com.example.incidentintake.chat.ChatResponse;
import com.example.incidentintake.chat.ChatUpstreamException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.retry.NonTransientAiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final String VALID_API_KEY = "sk-ant-test-key";

    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callResponseSpec;
    @Mock ChatMemory chatMemory;

    ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatClient, chatMemory, VALID_API_KEY);
    }

    private void stubHappyPath() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void chat_withoutConversationId_generatesNewOne() {
        stubHappyPath();
        when(callResponseSpec.content()).thenReturn("Hello, how can I help?");

        ChatResponse response = controller.chat(new ChatRequest("Hi", null));

        assertThat(response.reply()).isEqualTo("Hello, how can I help?");
        assertThat(response.conversationId()).isNotBlank();
    }

    @Test
    void chat_withConversationId_reusesIt() {
        stubHappyPath();
        when(callResponseSpec.content()).thenReturn("Sure, here's the list.");

        ChatResponse response = controller.chat(new ChatRequest("List incidents", "conv-42"));

        assertThat(response.conversationId()).isEqualTo("conv-42");
    }

    @Test
    void chat_passesUserMessageToChatClient() {
        stubHappyPath();
        when(callResponseSpec.content()).thenReturn("ok");

        controller.chat(new ChatRequest("Create an incident for DB outage", "conv-1"));

        verify(requestSpec).user("Create an incident for DB outage");
    }

    @Test
    void chat_attachesConversationMemoryAdvisor() {
        stubHappyPath();
        when(callResponseSpec.content()).thenReturn("ok");

        controller.chat(new ChatRequest("What's the status?", "conv-7"));

        verify(requestSpec).advisors(any(Advisor.class));
    }

    @Test
    void chat_returnsModelReplyVerbatim() {
        stubHappyPath();
        when(callResponseSpec.content()).thenReturn("Incident INC-123 is now IN_PROGRESS.");

        ChatResponse response = controller.chat(new ChatRequest("Update status", "conv-9"));

        assertThat(response.reply()).isEqualTo("Incident INC-123 is now IN_PROGRESS.");
    }

    @Test
    void chat_twoCallsWithoutConversationId_generateDifferentIds() {
        stubHappyPath();
        when(callResponseSpec.content()).thenReturn("ok");

        ChatResponse first = controller.chat(new ChatRequest("Hi", null));
        ChatResponse second = controller.chat(new ChatRequest("Hi again", null));

        assertThat(first.conversationId()).isNotEqualTo(second.conversationId());
    }

    // ── API key not configured ────────────────────────────────────────────

    @Test
    void chat_placeholderApiKey_throwsChatNotConfiguredWithoutCallingModel() {
        ChatController unconfigured = new ChatController(chatClient, chatMemory,
                "REPLACE_WITH_YOUR_ANTHROPIC_API_KEY");

        assertThatThrownBy(() -> unconfigured.chat(new ChatRequest("Hi", null)))
                .isInstanceOf(ChatNotConfiguredException.class)
                .hasMessageContaining("spring.ai.anthropic.api-key")
                .hasMessageContaining("restart");

        verifyNoInteractions(chatClient);
    }

    @Test
    void chat_blankApiKey_throwsChatNotConfigured() {
        ChatController unconfigured = new ChatController(chatClient, chatMemory, "  ");

        assertThatThrownBy(() -> unconfigured.chat(new ChatRequest("Hi", null)))
                .isInstanceOf(ChatNotConfiguredException.class);

        verifyNoInteractions(chatClient);
    }

    @Test
    void chat_nullApiKey_throwsChatNotConfigured() {
        ChatController unconfigured = new ChatController(chatClient, chatMemory, null);

        assertThatThrownBy(() -> unconfigured.chat(new ChatRequest("Hi", null)))
                .isInstanceOf(ChatNotConfiguredException.class);

        verifyNoInteractions(chatClient);
    }

    // ── Upstream (Anthropic) failures ───────────────────────────────────────

    @Test
    void chat_nonTransientAiException_wrappedAsChatUpstreamException() {
        stubHappyPath();
        when(callResponseSpec.content()).thenThrow(new NonTransientAiException("401 Unauthorized: invalid x-api-key"));

        assertThatThrownBy(() -> controller.chat(new ChatRequest("Hi", null)))
                .isInstanceOf(ChatUpstreamException.class)
                .hasMessageContaining("Anthropic")
                .hasCauseInstanceOf(NonTransientAiException.class);
    }
}
