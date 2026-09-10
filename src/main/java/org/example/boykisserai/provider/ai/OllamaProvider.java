package org.example.boykisserai.provider.ai;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.domain.model.ChatContext;
import org.example.boykisserai.domain.model.ChatResponse;
import org.example.boykisserai.domain.model.Message;
import org.example.boykisserai.domain.model.Ollama.OllamaChatRequest;
import org.example.boykisserai.domain.model.Ollama.OllamaChatResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "qwen", matchIfMissing = true)
public class OllamaProvider implements AiProvider {

    private final WebClient ollamaWebClient;
    private final AppProperties props;

    public OllamaProvider(WebClient ollamaWebClient, AppProperties props) {
        this.ollamaWebClient = ollamaWebClient;
        this.props = props;
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

        String model = props.getOllama().getModelName();
        // format="json" подставляется только если context.expectJson() — та же
        // причина, что и у OpenAI-провайдера: FactExtractorService ждёт обычный
        // текст ("FACT: ..."), а не JSON.
        OllamaChatRequest request = new OllamaChatRequest(model, messages, false, context.expectJson());

        log.info("Отправка запроса в локальную Ollama ({})...", model);

        try {
            OllamaChatResponse response = ollamaWebClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .block();

            if (response != null && response.message() != null) {
                return new ChatResponse(response.message().content());
            }
        } catch (Exception e) {
            log.error("Ошибка получения ответа от Ollama: {}", e.getMessage(), e);
        }

        return new ChatResponse(context.expectJson()
                ? "{\"text\":\"Ошибка получения ответа от локальной Qwen.\",\"emotion\":\"JOY\",\"trust_delta\":0,\"reason\":\"Ошибка API\",\"action\":\"NONE\"}"
                : "NONE");
    }

    @Override
    public boolean supports(String providerName) {
        return "qwen".equalsIgnoreCase(providerName);
    }
}