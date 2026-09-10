package org.example.boykisserai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    // Провайдеры
    private Ai ai = new Ai();
    private Stt stt = new Stt();
    private Tts tts = new Tts();

    // Логика бота и звук
    private Bot bot = new Bot();
    private Audio audio = new Audio();

    // Внешние сервисы
    private OpenAi openai = new OpenAi();
    private Ollama ollama = new Ollama();
    private Discord discord = new Discord();
    private Vts vts = new Vts();
    private Yandex yandex = new Yandex();
    private Twitch twitch = new Twitch();

    @Getter @Setter
    public static class Ai {
        private String provider = "openai";
    }

    @Getter @Setter
    public static class Stt {
        private String provider = "openai";
    }

    @Getter @Setter
    public static class Tts {
        private String provider = "yandex";
    }

    @Getter @Setter
    public static class Bot {
        private String creatorDiscordId = "614169906623021081";
        private long activeDialogWindowMs = 25000;
    }

    @Getter @Setter
    public static class Audio {
        private double micGain = 2.2;
        private double speechThreshold = 300.0;
        private long silenceTimeoutMs = 500;
        private int prerollBufferCount = 5;
    }

    @Getter @Setter
    public static class OpenAi {
        private String baseUrl = "https://api.openai.com";
        private String modelName = "gpt-4o-mini";
        private String apiKey = "";
        private String voice = "nova";
        private double ttsSpeed = 1.05;
    }

    @Getter @Setter
    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String modelName = "qwen2.5:7b";
    }

    @Getter @Setter
    public static class Discord {
        private String token = "";
    }

    @Getter @Setter
    public static class Vts {
        private String websocketUrl = "ws://localhost:8001";
    }

    @Getter @Setter
    public static class Yandex {
        private String apiKey = "";
        private String voice = "jane";
        private String emotion = "evil";
        private String speed = "1.10";
        private double pitchShift = -0.8;
    }

    @Getter @Setter
    public static class Twitch {
        private boolean enabled = false;
        private String channel = "";
        private String oauthToken = "";
    }
}