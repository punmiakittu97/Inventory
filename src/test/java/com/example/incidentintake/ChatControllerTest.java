package com.example.incidentintake;

import com.example.incidentintake.chat.ChatController;
import com.example.incidentintake.chat.ChatRequest;
import com.example.incidentintake.chat.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callResponseSpec;
    @Mock ChatMemory chatMemory;

    ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatClient, chatMemory);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Advisor.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void chat_withoutConversationId_generatesNewOne() {
        when(callResponseSpec.content()).thenReturn("Hello, how can I help?");

        ChatResponse response = controller.chat(new ChatRequest("Hi", null));

        assertThat(response.reply()).isEqualTo("Hello, how can I help?");
        assertThat(response.conversationId()).isNotBlank();
    }

    @Test
    void chat_withConversationId_reusesIt() {
        when(callResponseSpec.content()).thenReturn("Sure, here's the list.");

        ChatResponse response = controller.chat(new ChatRequest("List incidents", "conv-42"));

        assertThat(response.conversationId()).isEqualTo("conv-42");
    }

    @Test
    void chat_passesUserMessageToChatClient() {
        when(callResponseSpec.content()).thenReturn("ok");

        controller.chat(new ChatRequest("Create an incident for DB outage", "conv-1"));

        verify(requestSpec).user("Create an incident for DB outage");
    }

    @Test
    void chat_attachesConversationMemoryAdvisor() {
        when(callResponseSpec.content()).thenReturn("ok");

        controller.chat(new ChatRequest("What's the status?", "conv-7"));

        verify(requestSpec).advisors(any(Advisor.class));
    }

    @Test
    void chat_returnsModelReplyVerbatim() {
        when(callResponseSpec.content()).thenReturn("Incident INC-123 is now IN_PROGRESS.");

        ChatResponse response = controller.chat(new ChatRequest("Update status", "conv-9"));

        assertThat(response.reply()).isEqualTo("Incident INC-123 is now IN_PROGRESS.");
    }

    @Test
    void chat_twoCallsWithoutConversationId_generateDifferentIds() {
        when(callResponseSpec.content()).thenReturn("ok");

        ChatResponse first = controller.chat(new ChatRequest("Hi", null));
        ChatResponse second = controller.chat(new ChatRequest("Hi again", null));

        assertThat(first.conversationId()).isNotEqualTo(second.conversationId());
    }
}
