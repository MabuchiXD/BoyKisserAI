package org.example.boykisserai.provider.ai;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.domain.model.ChatContext;
import org.example.boykisserai.domain.model.ChatResponse;
import org.example.boykisserai.domain.model.Message;
import org.example.boykisserai.domain.model.OpenAi.OpenAiChatRequest;
import org.example.boykisserai.domain.model.OpenAi.OpenAiChatResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openai")
public class OpenAiProvider implements AiProvider {

    private final WebClient webClient;
    private final AppProperties props;


    private static final int MAX_EMPTY_RETRIES = 1;

    public OpenAiProvider(AppProperties props) {
        this.props = props;
        this.webClient = WebClient.builder().baseUrl(props.getOpenai().getBaseUrl()).build();
    }

    @Override
    public ChatResponse generate(ChatContext context) {
        List<Message> messages = new ArrayList<>();

        if (context.systemPrompt() != null && !context.systemPrompt().isEmpty()) {
            messages.add(new Message("system", context.systemPrompt()));
        }
        if (context.history() != null) {
            messages.addAll(context.history());
        }
        messages.add(new Message("user", context.userMessage()));

        String model = props.getOpenai().getModelName();
        OpenAiChatRequest request = new OpenAiChatRequest(model, messages, 0.7, context.expectJson());

        String content = null;

        for (int attempt = 0; attempt <= MAX_EMPTY_RETRIES; attempt++) {
            if (attempt == 0) {
                log.info("Отправка запроса в OpenAI ({}) для пользователя...", model);
            } else {
                log.warn("Модель вернула пустой ответ, повторная попытка ({}/{})...", attempt, MAX_EMPTY_RETRIES);
            }

            try {
                OpenAiChatResponse response = webClient.post()
                        .uri("/v1/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getOpenai().getApiKey())
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(OpenAiChatResponse.class)
                        .block();

                if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                    content = response.choices().get(0).message().content();
                }
            } catch (Exception e) {
                log.error("Ошибка получения ответа от OpenAI: {}", e.getMessage(), e);
                break; // сетевая/API-ошибка — retry тут не поможет, сразу уходим в fallback
            }

            if (content != null && !content.isBlank()) {
                return new ChatResponse(content);
            }
        }

        return new ChatResponse(context.expectJson()
                ? "{\"text\":\"Ошибка генерации ответа от OpenAI.\",\"emotion\":\"JOY\",\"trust_delta\":0,\"reason\":\"Ошибка API\",\"action\":\"NONE\"}"
                : "NONE");
    }

    @Override
    public boolean supports(String providerName) {
        return "openai".equalsIgnoreCase(providerName);
    }
}