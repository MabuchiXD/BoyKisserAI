package org.example.boykisserai.service.ai;

import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.domain.model.ChatContext;
import org.example.boykisserai.domain.model.ChatResponse;
import org.example.boykisserai.domain.model.Message;
import org.example.boykisserai.provider.ai.AiProvider;
import org.example.boykisserai.service.memory.MemoryService;
import org.example.boykisserai.service.prompt.PromptService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiService {

    private final AiProvider aiProvider;
    private final PromptService promptService;
    private final MemoryService memoryService;

    public AiService(AiProvider aiProvider,
                     PromptService promptService,
                     MemoryService memoryService) {
        this.aiProvider = aiProvider;
        this.promptService = promptService;
        this.memoryService = memoryService;
    }

    public String generateReply(UserEntity user, String userInput) {
        // 1. Достаем историю диалога из Redis
        List<Message> shortTermHistory = memoryService.getShortTermHistory(user.getExternalId());

        // 2. Собираем умный промпт с учетом памяти и отношений
        String systemPrompt = promptService.buildSystemPrompt(user, userInput);

        // PromptService требует от модели строго JSON-объект (text/emotion/trust_delta/
        // reason/action) — поэтому здесь expectJson=true, в отличие от FactExtractorService,
        // у которого свой промпт и обычный текстовый формат ответа
        ChatContext context = new ChatContext(systemPrompt, shortTermHistory, userInput, true);

        // 3. Генерируем ответ через активный AiProvider (Ollama Qwen или OpenAI)
        ChatResponse response = aiProvider.generate(context);

        return response.content();
    }
}