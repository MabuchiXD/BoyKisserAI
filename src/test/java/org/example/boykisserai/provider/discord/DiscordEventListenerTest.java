package org.example.boykisserai.provider.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Mentions;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.example.boykisserai.service.CharacterService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Отключает придирки Mockito к неиспользованным стабам
class DiscordEventListenerTest {

    @Mock
    private CharacterService characterService;

    @Mock
    private DashboardStateService dashboardStateService;

    @Mock
    private MessageReceivedEvent event;

    @Mock
    private Message discordMessage;

    @Mock
    private User author;

    @Mock
    private MessageChannelUnion channel;

    @Mock
    private MessageCreateAction messageCreateAction;

    @Mock
    private Mentions mentions;

    @Mock
    private JDA jda;

    @Mock
    private SelfUser selfUser;

    private static final String BOT_ID = "1531085093601415208";

    @InjectMocks
    private DiscordEventListener discordEventListener;

    @BeforeEach
    void setUp() {
        when(event.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(false);
        when(event.getMessage()).thenReturn(discordMessage);
        when(event.getChannel()).thenReturn(channel);
        when(channel.sendMessage(anyString())).thenReturn(messageCreateAction);
        doNothing().when(messageCreateAction).queue();

        // РАНЬШЕ проверка упоминания шла через raw.contains("<@") — теперь через
        // event.getMessage().getMentions().getUsers(), поэтому это нужно мокать
        // в КАЖДОМ тесте, иначе NullPointerException на getMentions() == null.
        // По умолчанию — никто не упомянут; конкретный тест на упоминание
        // переопределяет это ниже.
        when(discordMessage.getMentions()).thenReturn(mentions);
        when(mentions.getUsers()).thenReturn(List.of());
    }

    @Test
    @DisplayName("ТЕГ @БОТА: Должен распознавать настоящее упоминание именно бота и очищать тег из текста")
    void shouldProcessMessageWhenBotIsMentioned() throws Exception {
        when(dashboardStateService.isDiscordMuted()).thenReturn(false);
        when(discordMessage.getContentRaw()).thenReturn("<@" + BOT_ID + "> привет!");
        when(author.getId()).thenReturn("349937009399300096");
        when(author.getEffectiveName()).thenReturn("AsataEnot");
        when(event.isFromGuild()).thenReturn(true);

        // Настоящее упоминание: среди упомянутых пользователей есть сам бот
        User mentionedBotUser = mock(User.class);
        when(mentionedBotUser.getId()).thenReturn(BOT_ID);
        when(mentions.getUsers()).thenReturn(List.of(mentionedBotUser));

        when(event.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(selfUser.getId()).thenReturn(BOT_ID);

        when(characterService.chat("349937009399300096", "AsataEnot", "DISCORD", "привет!"))
                .thenReturn("Привет, AsataEnot!");

        discordEventListener.onMessageReceived(event);
        Thread.sleep(150); // Даем асинхронному потоку завершиться

        verify(characterService, times(1)).chat("349937009399300096", "AsataEnot", "DISCORD", "привет!");
        verify(channel, times(1)).sendMessage("Привет, AsataEnot!");
    }

    @Test
    @DisplayName("ЧУЖОЙ ТЕГ: НЕ должен отвечать, если в сообщении упомянут другой пользователь, а не бот")
    void shouldNotRespondWhenSomeoneElseIsMentioned() throws Exception {
        when(dashboardStateService.isDiscordMuted()).thenReturn(false);
        when(discordMessage.getContentRaw()).thenReturn("<@999999999999999999> глянь сюда");
        when(event.isFromGuild()).thenReturn(true);

        // Упомянут кто-то другой, не бот
        User someoneElse = mock(User.class);
        when(someoneElse.getId()).thenReturn("999999999999999999");
        when(mentions.getUsers()).thenReturn(List.of(someoneElse));

        when(event.getJDA()).thenReturn(jda);
        when(jda.getSelfUser()).thenReturn(selfUser);
        when(selfUser.getId()).thenReturn(BOT_ID);

        discordEventListener.onMessageReceived(event);
        Thread.sleep(100);

        verify(characterService, never()).chat(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("ИМЯ В ТЕКСТЕ: Должен вызывать ИИ, если написано 'кот' или 'бойкиссер'")
    void shouldProcessMessageWhenNameIsUsed() throws Exception {
        when(dashboardStateService.isDiscordMuted()).thenReturn(false);
        when(discordMessage.getContentRaw()).thenReturn("кот, как дела?");
        when(author.getId()).thenReturn("112233");
        when(author.getEffectiveName()).thenReturn("Gleb");
        when(event.isFromGuild()).thenReturn(true);

        when(characterService.chat("112233", "Gleb", "DISCORD", "кот, как дела?"))
                .thenReturn("Всё супер!");

        discordEventListener.onMessageReceived(event);
        Thread.sleep(150);

        verify(characterService, times(1)).chat("112233", "Gleb", "DISCORD", "кот, как дела?");
        verify(channel, times(1)).sendMessage("Всё супер!");
    }

    @Test
    @DisplayName("МУТ ДИСКОРДА: Должен полностью игнорировать сообщения, если Discord выключен на панели")
    void shouldIgnoreWhenDiscordIsMuted() throws Exception {
        when(dashboardStateService.isDiscordMuted()).thenReturn(true);

        discordEventListener.onMessageReceived(event);
        Thread.sleep(50);

        verify(characterService, never()).chat(anyString(), anyString(), anyString(), anyString());
    }
}