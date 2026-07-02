package com.example.incidentintake.chat;

public class ChatNotConfiguredException extends RuntimeException {

    public ChatNotConfiguredException() {
        super("Anthropic API key is not configured. Set spring.ai.anthropic.api-key "
                + "(or the ANTHROPIC_API_KEY environment variable) in application.properties, "
                + "then restart the application.");
    }
}
