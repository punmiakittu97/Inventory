package com.example.incidentintake.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ChatController {

    // Matches the default in application.properties when ANTHROPIC_API_KEY is unset
    static final String PLACEHOLDER_API_KEY = "REPLACE_WITH_YOUR_ANTHROPIC_API_KEY";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final String anthropicApiKey;

    public ChatController(ChatClient chatClient,
                           ChatMemory chatMemory,
                           @Value("${spring.ai.anthropic.api-key}") String anthropicApiKey) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.anthropicApiKey = anthropicApiKey;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (!StringUtils.hasText(anthropicApiKey) || PLACEHOLDER_API_KEY.equals(anthropicApiKey)) {
            throw new ChatNotConfiguredException();
        }

        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        String reply;
        try {
            reply = chatClient.prompt()
                    .user(request.message())
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                            .conversationId(conversationId)
                            .build())
                    .call()
                    .content();
        } catch (NonTransientAiException | TransientAiException e) {
            throw new ChatUpstreamException(
                    "The chat service could not complete the request via Anthropic: " + e.getMessage(), e);
        }

        return new ChatResponse(reply, conversationId);
    }
}
