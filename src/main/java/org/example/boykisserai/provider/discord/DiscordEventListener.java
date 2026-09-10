package org.example.boykisserai.provider.discord;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.boykisserai.service.CharacterService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class DiscordEventListener extends ListenerAdapter {

    private final CharacterService characterService;
    private final DashboardStateService dashboardStateService;

    public DiscordEventListener(CharacterService characterService, DashboardStateService dashboardStateService) {
        this.characterService = characterService;
        this.dashboardStateService = dashboardStateService;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (dashboardStateService.isDiscordMuted()) {
            return;
        }

        String raw = event.getMessage().getContentRaw().trim();
        String lower = raw.toLowerCase();

        boolean mentionsThisBot = event.getMessage().getMentions().getUsers().stream()
                .anyMatch(user -> user.getId().equals(event.getJDA().getSelfUser().getId()));

        boolean shouldRespond = lower.contains("бойкиссер") ||
                lower.contains("boykisser") ||
                lower.contains("@boykisser") ||
                lower.contains("кот") ||
                lower.contains("бот") ||
                raw.startsWith("!") ||
                mentionsThisBot ||
                !event.isFromGuild();

        if (shouldRespond) {
            String discordUserId = event.getAuthor().getId();
            String discordName = event.getAuthor().getEffectiveName();

            String cleanInput = raw.replaceAll("<@!?\\d+>", "")
                    .replaceAll("@\\S+", "")
                    .replaceAll("^!+", "")
                    .trim();

            if (cleanInput.isEmpty()) cleanInput = "Привет!";
            final String userMessage = cleanInput;

            log.info("[Discord Event] Входящее сообщение от {}: \"{}\"", discordName, raw);

            CompletableFuture.runAsync(() -> {
                try {
                    String replyText = characterService.chat(discordUserId, discordName, "DISCORD", userMessage);

                    if (replyText != null && !replyText.isBlank()) {
                        event.getChannel().sendMessage(replyText).queue(
                                success -> log.info("[Discord OK] Ответ успешно отправлен в канал!"),
                                error -> log.error("[Discord Error] Ошибка отправки в чат: {}", error.getMessage())
                        );
                    }
                } catch (Exception e) {
                    log.error("[Discord Error] Сбой обработки: {}", e.getMessage());
                }
            });
        }
    }
}