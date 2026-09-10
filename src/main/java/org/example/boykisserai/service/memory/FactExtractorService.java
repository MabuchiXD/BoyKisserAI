package org.example.boykisserai.service.memory;

import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.domain.constant.MemoryType;
import org.example.boykisserai.domain.entity.UserEntity;
import org.example.boykisserai.domain.model.ChatContext;
import org.example.boykisserai.domain.model.ChatResponse;
import org.example.boykisserai.provider.ai.AiProvider;
import org.example.boykisserai.service.state.DashboardStateService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FactExtractorService {

    private final AiProvider aiProvider;
    private final MemoryService memoryService;
    private final DashboardStateService dashboardStateService;

    public FactExtractorService(AiProvider aiProvider,
                                MemoryService memoryService,
                                DashboardStateService dashboardStateService) {
        this.aiProvider = aiProvider;
        this.memoryService = memoryService;
        this.dashboardStateService = dashboardStateService;
    }

    @Async("vtuberAsyncExecutor")
    public void extractAndSaveFacts(UserEntity user, String userMessage) {
        try {
            String filterPrompt = """
                    Ты — автоматический модуль анализа долговременной памяти. Проанализируй сообщение пользователя.

                    ПРАВИЛА АНАЛИЗА:
                    1. FACT: Сохраняй ТОЛЬКО реальную полезную информацию о человеке (его имя, хобби, предпочтения, работа, личные вкусы).
                       Формат: FACT: <суть факта>
                    
                    2. BLUNDER: Фиксируй ТОЛЬКО реальные факапы (человек сломал прод, крупно опозорился) или ПРЯМЫЕ токсичные оскорбления В АДРЕС БОТА.
                       Формат: BLUNDER: <суть косяка или оскорбления>

                    3. ВНИМАНИЕ (ЧТО ИГНОРИРОВАТЬ):
                       - Пошлые шутки, мат в обычной речи, рофлы, разговоры про секс, мемы и дружеский стёб — это НЕ оскорбление и НЕ косяк! Отвечай СТРОГО: NONE.
                       - Обычный треп, приветствия и нейтральные фразы — отвечай СТРОГО: NONE.

                    Отвечай СТРОГО на русском языке: FACT: ..., BLUNDER: ... или NONE.
                    """;

            ChatContext context = new ChatContext(filterPrompt, null, userMessage);
            ChatResponse response = aiProvider.generate(context);

            if (response == null || response.content() == null) return;
            String result = response.content().trim();

            boolean isPrivileged = user.isCreator() || "dina".equalsIgnoreCase(user.getExternalId());
            if (isPrivileged && result.startsWith("BLUNDER:")) {
                return;
            }

            log.debug("[FactExtractor] Результат анализа: {}", result);

            if (result.startsWith("FACT:")) {
                String factText = result.replace("FACT:", "").trim();
                if (factText.length() > 3) {
                    memoryService.saveFact(user, factText, MemoryType.FACT);
                    dashboardStateService.addMemoryNotification(user.getDisplayName(), factText, MemoryType.FACT);
                }
            } else if (result.startsWith("BLUNDER:")) {
                String blunderText = result.replace("BLUNDER:", "").trim();
                if (blunderText.length() > 3) {
                    memoryService.saveFact(user, blunderText, MemoryType.BLUNDER);
                    dashboardStateService.addMemoryNotification(user.getDisplayName(), blunderText, MemoryType.BLUNDER);
                }
            }
        } catch (Exception e) {
            log.warn("[FactExtractor] Ошибка фонового анализа памяти: {}", e.getMessage());
        }
    }
}