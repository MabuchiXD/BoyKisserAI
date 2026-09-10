package org.example.boykisserai.provider.twitch;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.common.events.domain.EventUser;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.service.CharacterService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwitchChatServiceTest {

    @Mock
    private CharacterService characterService;

    @Mock
    private DashboardStateService dashboardStateService;

    @Mock
    private TwitchClient twitchClient;

    @Mock
    private TwitchChat twitchChat;

    @Mock
    private ChannelMessageEvent event;

    @Mock
    private EventUser eventUser;

    // AppProperties — простой конфиг-POJO, реального смысла его мокать нет.
    // Раньше тест дергал ReflectionTestUtils.setField(..., "channelName", ...) —
    // такого поля в TwitchChatService не существует (канал берётся из
    // props.getTwitch().getChannel()), поэтому setField падал с исключением
    // прямо в setUp() и валил разом все тесты класса. Собираем сервис вручную
    // с реальным AppProperties вместо @InjectMocks.
    private TwitchChatService twitchChatService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getTwitch().setChannel("mabuchi");

        twitchChatService = new TwitchChatService(props, characterService, dashboardStateService);
        ReflectionTestUtils.setField(twitchChatService, "twitchClient", twitchClient);

        when(dashboardStateService.isTwitchMuted()).thenReturn(false);
    }

    @Test
    @DisplayName("ТЕГ ЗРИТЕЛЯ: Должен обязательно добавлять @ник зрителя при ответе в чат Twitch")
    void shouldMentionViewerNicknameWithAtSymbolInChat() throws Exception {
        when(event.getUser()).thenReturn(eventUser);
        when(eventUser.getName()).thenReturn("Sanya228");
        when(eventUser.getId()).thenReturn("778899");
        when(event.getMessage()).thenReturn("Кот, когда стрим закончишь?");
        when(characterService.chat("778899", "Sanya228", "TWITCH", "Кот, когда стрим закончишь?"))
                .thenReturn("Sanya228, да пошел ты, я только разогнался!");
        when(twitchClient.getChat()).thenReturn(twitchChat);

        // Симулируем входящее сообщение со стрима
        twitchChatService.onMessageReceived(event);
        Thread.sleep(100);

        // Проверяем: в чат Твича должно уйти сообщение с собачкой @Sanya228!
        verify(twitchChat, times(1)).sendMessage("mabuchi", "@Sanya228 Sanya228, да пошел ты, я только разогнался!");
    }

    @Test
    @DisplayName("КОМАНДА С '!': Должен вырезать '!' и отвечать с тегом @ника")
    void shouldProcessExclamationCommandWithMention() throws Exception {
        when(event.getUser()).thenReturn(eventUser);
        when(eventUser.getName()).thenReturn("ViewerX");
        when(eventUser.getId()).thenReturn("112233");
        when(event.getMessage()).thenReturn("!анекдот");
        when(characterService.chat("112233", "ViewerX", "TWITCH", "анекдот"))
                .thenReturn("ViewerX, колобок повесился.");
        when(twitchClient.getChat()).thenReturn(twitchChat);

        twitchChatService.onMessageReceived(event);
        Thread.sleep(100);

        verify(twitchChat, times(1)).sendMessage("mabuchi", "@ViewerX ViewerX, колобок повесился.");
    }

    @Test
    @DisplayName("МУТ КАНАЛА: Не должен отправлять сообщения, если Twitch заглушен на панели")
    void shouldNotProcessWhenTwitchIsMuted() throws Exception {
        when(dashboardStateService.isTwitchMuted()).thenReturn(true);

        twitchChatService.onMessageReceived(event);
        Thread.sleep(50);

        verify(characterService, never()).chat(anyString(), anyString(), anyString(), anyString());
    }
}