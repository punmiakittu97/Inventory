package com.example.incidentintake.config;

import com.example.incidentintake.mcp.IncidentMcpTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, IncidentMcpTools incidentMcpTools) {
        return builder
                .defaultSystem("""
                        You are an incident management assistant. You help teams track and resolve \
                        production incidents using the incident intake system.
                        You have tools to create incidents, list them with filters, update their \
                        status, and view their audit history.
                        Be concise and factual. Confirm before making any status changes.\
                        """)
                .defaultTools(incidentMcpTools)
                .build();
    }
}
