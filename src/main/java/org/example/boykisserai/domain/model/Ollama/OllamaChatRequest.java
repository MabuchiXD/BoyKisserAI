package org.example.boykisserai.domain.model.Ollama;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.boykisserai.domain.model.Message;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(
        String model,
        List<Message> messages,
        boolean stream,
        Map<String, Object> options,
        String format
) {
    public OllamaChatRequest(String model, List<Message> messages, boolean stream, boolean expectJson) {
        this(model, messages, stream, Map.of("temperature", 0.65), expectJson ? "json" : null);
    }
}