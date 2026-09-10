package org.example.boykisserai.domain.model.OpenAi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.boykisserai.domain.model.Message;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatRequest(
        String model,
        List<Message> messages,
        double temperature,
        // JSON формат по принуждению
        @JsonProperty("presence_penalty") double presencePenalty,
        @JsonProperty("frequency_penalty") double frequencyPenalty,
        @JsonProperty("response_format") Map<String, String> responseFormat
) {
    private static final Map<String, String> JSON_OBJECT_FORMAT = Map.of("type", "json_object");

    public OpenAiChatRequest(String model, List<Message> messages, double temperature, boolean expectJson) {
        this(model, messages, temperature, 0.7, 0.6, expectJson ? JSON_OBJECT_FORMAT : null);
    }
}