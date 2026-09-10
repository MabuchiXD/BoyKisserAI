package org.example.boykisserai.provider.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.service.CharacterService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.twitch.enabled", havingValue = "true", matchIfMissing = false)
public class TwitchChatService {

    private final AppProperties props;
    private final CharacterService characterService;
    private final DashboardStateService dashboardStateService;
    private TwitchClient twitchClient;

    public TwitchChatService(AppProperties props,
                             CharacterService characterService,
                             DashboardStateService dashboardStateService) {
        this.props = props;
        this.characterService = characterService;
        this.dashboardStateService = dashboardStateService;
    }

    @PostConstruct
    public void init() {
        String token = props.getTwitch().getOauthToken();
        String channel = props.getTwitch().getChannel();

        if (token == null || token.isBlank() || token.contains("ВСТАВЬ")) {
            log.warn("[Twitch] OAuth-токен не указан. Пропуск запуска Twitch.");
            return;
        }

        try {
            log.info("[Twitch] Подключение к чату канала: #{}...", channel);

            OAuth2Credential credential = new OAuth2Credential("twitch", token.replace("oauth:", ""));

            twitchClient = TwitchClientBuilder.builder()
                    .withEnableChat(true)
                    .withChatAccount(credential)
                    .build();

            twitchClient.getChat().joinChannel(channel);
            twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, this::onMessageReceived);

            log.info("[Twitch OK] УСПЕШНО ПОДКЛЮЧЕН К ЧАТУ: #{}", channel);

        } catch (Exception e) {
            log.error("[Twitch Error] Ошибка подключения к Twitch: {}", e.getMessage());
        }
    }

    void onMessageReceived(ChannelMessageEvent event) {
        if (dashboardStateService.isTwitchMuted()) {
            return;
        }

        String viewerName = event.getUser().getName();
        String viewerId = event.getUser().getId();
        String message = event.getMessage().trim();
        String channel = props.getTwitch().getChannel();

        if (viewerName.equalsIgnoreCase(channel) && message.startsWith("[Boykisser]")) {
            return;
        }

        String lower = message.toLowerCase();
        boolean hasWakeWord = lower.contains("бойкиссер") ||
                lower.contains("кот") ||
                lower.contains("бот") ||
                message.startsWith("!");

        if (hasWakeWord) {
            CompletableFuture.runAsync(() -> {
                String cleanInput = message.startsWith("!") ? message.substring(1).trim() : message;
                String replyText = characterService.chat(viewerId, viewerName, "TWITCH", cleanInput);

                if (replyText != null && !replyText.isBlank()) {
                    twitchClient.getChat().sendMessage(channel, "@" + viewerName + " " + replyText);
                }
            });
        }
    }

    //Чтобы бот плавно отключался от чата твича при выключении приложения
    @PreDestroy
    public void cleanup() {
        if (twitchClient != null) {
            twitchClient.close();
            log.info("[Twitch] Соединение с чатом закрыто.");
        }
    }
}