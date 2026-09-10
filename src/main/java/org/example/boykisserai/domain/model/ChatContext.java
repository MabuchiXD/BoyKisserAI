package org.example.boykisserai.domain.model;

import java.util.List;

public record ChatContext(
        String systemPrompt,
        List<Message> history,
        String userMessage,
        boolean expectJson
) {
    public ChatContext(String systemPrompt, List<Message> history, String userMessage) {
        this(systemPrompt, history, userMessage, false);
    }
}