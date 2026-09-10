package org.example.boykisserai.domain.model.Ollama;

import org.example.boykisserai.domain.model.Message;

public record OllamaChatResponse(
        String model,
        Message message,
        boolean done
) {}