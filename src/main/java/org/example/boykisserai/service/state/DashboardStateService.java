package org.example.boykisserai.service.state;

import lombok.Getter;
import lombok.Setter;
import org.example.boykisserai.domain.constant.EmotionType;
import org.example.boykisserai.domain.constant.MemoryType;
import org.example.boykisserai.domain.constant.PersonalityPhase;
import org.example.boykisserai.domain.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Service
@Getter
@Setter
public class DashboardStateService {

    // Флаги активности каналов (для режима стрима)
    private volatile boolean discordMuted = false;
    private volatile boolean twitchMuted = false;

    private int emotionIntensity = 35;
    private final long sessionStartTime = System.currentTimeMillis();
    private int totalMessageCount = 0;

    private UserEntity currentActiveUser;
    private String currentSpeaker = "Мабучи";
    private String currentPlatform = "LOCAL";
    private int trustLevel = 100;
    private String relationship = "Создатель";

    private EmotionType currentEmotion = EmotionType.JOY;
    private PersonalityPhase currentPhase = PersonalityPhase.CYNIC_MEME;
    private boolean forceEmotionOverride = false;

    private volatile double currentMicVolume = 0.0;
    private volatile boolean micMuted = false;
    private volatile long dialogWindowExpireTime = 0;

    // Память свежего конфликта
    private volatile String lastDramaIncident = null;
    private volatile long lastDramaTimestamp = 0;
    private volatile boolean dramaVentedToCreator = false;
    private static final long DRAMA_TTL_MS = 3 * 60 * 1000;

    private final LinkedList<ChatMessageItem> chatMessages = new LinkedList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public record ChatMessageItem(
            String timestamp,
            String sender,
            String platform,
            String text,
            boolean isAi,
            String mood,
            int trustDelta,
            String reason,
            String action // Новое поле действия: SLEEP, WAKE, NONE
    ) {}

    public synchronized void logUserMessage(String time, String sender, String platform, String text) {
        if (chatMessages.size() >= 80) chatMessages.removeFirst();
        chatMessages.add(new ChatMessageItem(time, sender, platform, text, false, "", 0, "", "NONE"));
        totalMessageCount++;
    }

    public synchronized void logAiResponse(String time, String platform, String text, String mood, int trustDelta, String reason, String action) {
        if (chatMessages.size() >= 80) chatMessages.removeFirst();
        chatMessages.add(new ChatMessageItem(time, "Boykisser", platform, text, true, mood, trustDelta, reason, action));
    }

    public synchronized void addMemoryNotification(String username, String memoryText, MemoryType type) {
        String time = timeFormat.format(new Date());
        String tag = type == MemoryType.BLUNDER ? "⚠️ КОМПРОМАТ" : "📌 ФАКТ";
        logAiResponse(time, "SYSTEM", String.format("%s о %s: \"%s\"", tag, username, memoryText),
                type == MemoryType.BLUNDER ? "IRRITATION" : "TRUST", 0, "Занесено в долговременную память PostgreSQL", "NONE");
    }

    public void recordDrama(String culpritName, String platform, String culpritText, String reason) {
        this.lastDramaIncident = String.format("Пользователь %s в %s только что выбесил тебя фразой: \"%s\" (Причина: %s)",
                culpritName, platform, culpritText, reason);
        this.lastDramaTimestamp = System.currentTimeMillis();
        this.dramaVentedToCreator = false;
    }

    public String getActiveDramaForCreator() {
        boolean isFresh = (System.currentTimeMillis() - lastDramaTimestamp) < DRAMA_TTL_MS;
        if (isFresh && !dramaVentedToCreator && lastDramaIncident != null) {
            return lastDramaIncident;
        }
        return null;
    }

    public void markDramaAsVented() {
        this.dramaVentedToCreator = true;
        this.lastDramaIncident = null;
    }

    public boolean isDialogWindowActive() {
        return System.currentTimeMillis() < dialogWindowExpireTime;
    }

    public int getDialogWindowRemainingSeconds() {
        long remaining = (dialogWindowExpireTime - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remaining);
    }

    public synchronized List<ChatMessageItem> getAllMessagesCopy() {
        return List.copyOf(chatMessages);
    }

    public synchronized List<ChatMessageItem> getLocalMessages() {
        // В локальный чат попадают ТОЛЬКО реплики человека и ответы бота (без SYSTEM логов!)
        return chatMessages.stream()
                .filter(m -> "LOCAL".equalsIgnoreCase(m.platform()) || "LOCAL_MIC".equalsIgnoreCase(m.platform()))
                .toList();
    }

    public synchronized List<ChatMessageItem> getDiscordMessages() {
        return chatMessages.stream()
                .filter(m -> "DISCORD".equalsIgnoreCase(m.platform()) || "DISCORD_TEXT".equalsIgnoreCase(m.platform()))
                .toList();
    }

    public synchronized List<ChatMessageItem> getTwitchMessages() {
        return chatMessages.stream()
                .filter(m -> "TWITCH".equalsIgnoreCase(m.platform()))
                .toList();
    }
}