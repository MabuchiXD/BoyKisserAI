package org.example.boykisserai.domain.model.OpenAi;

import org.example.boykisserai.domain.model.Message;

import java.util.List;

public record OpenAiChatResponse(
        List<Choice> choices
) {
    public record Choice(
            Message message
    ) {}
}