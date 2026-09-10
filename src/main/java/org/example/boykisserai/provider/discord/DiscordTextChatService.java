package org.example.boykisserai.provider.discord;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.boykisserai.config.AppProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DiscordTextChatService {

    private final AppProperties props;
    private final DiscordEventListener discordEventListener;

    public DiscordTextChatService(AppProperties props, DiscordEventListener discordEventListener) {
        this.props = props;
        this.discordEventListener = discordEventListener;
    }

    @PostConstruct
    public void init() {
        String token = props.getDiscord().getToken();

        if (token == null || token.isBlank() || token.contains("ВСТАВЬ")) {
            log.warn("[Discord] Токен бота не указан в конфигурации. Пропуск запуска.");
            return;
        }

        try {
            log.info("[Discord] Запуск текстового бота Boykisser...");

            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGES
                    )
                    .addEventListeners(discordEventListener)
                    .build();

            jda.awaitReady();
            log.info("[Discord OK] БОТ УСПЕШНО ВОШЕЛ В СЕТЬ: {}", jda.getSelfUser().getName());

        } catch (Exception e) {
            log.error("[Discord Error] Сбой подключения JDA: {}", e.getMessage());
        }
    }
}