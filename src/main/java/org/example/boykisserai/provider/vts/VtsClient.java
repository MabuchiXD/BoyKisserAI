package org.example.boykisserai.provider.vts;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

@Slf4j
public class VtsClient implements WebSocket.Listener {

    private WebSocket webSocket;
    private static final Path TOKEN_FILE = Path.of("vts_token.txt");

    public void connect(String url) {
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(url), this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    if (Files.exists(TOKEN_FILE)) {
                        try {
                            String savedToken = Files.readString(TOKEN_FILE).trim();
                            log.info("[VTS] Найден сохраненный токен. Авторизуемся...");
                            authenticatePlugin(savedToken);
                        } catch (IOException e) {
                            requestAuthToken();
                        }
                    } else {
                        requestAuthToken();
                    }
                })
                .exceptionally(ex -> {
                    log.error("[VTS] Ошибка подключения: {}", ex.getMessage());
                    return null;
                });
    }

    private void requestAuthToken() {
        String json = """
                {
                  "apiName": "VTubeStudioPublicAPI",
                  "apiVersion": "1.0",
                  "requestID": "TokenReq",
                  "messageType": "AuthenticationTokenRequest",
                  "data": {
                    "pluginName": "BoyKisserAI",
                    "pluginDeveloper": "Developer"
                  }
                }
                """;
        send(json);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();

        if (message.contains("AuthenticationTokenResponse")) {
            if (message.contains("\"authenticationToken\":\"")) {
                String token = message.split("\"authenticationToken\":\"")[1].split("\"")[0];
                try { Files.writeString(TOKEN_FILE, token); } catch (IOException ignored) {}
                authenticatePlugin(token);
            }
        } else if (message.contains("AuthenticationResponse")) {
            if (message.contains("\"authenticated\":true")) {
                log.info("[VTS] Успешно авторизован!");
            } else {
                try { Files.deleteIfExists(TOKEN_FILE); } catch (Exception ignored) {}
                requestAuthToken();
            }
        }

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    private void authenticatePlugin(String token) {
        String json = """
                {
                  "apiName": "VTubeStudioPublicAPI",
                  "apiVersion": "1.0",
                  "requestID": "AuthReq",
                  "messageType": "AuthenticationRequest",
                  "data": {
                    "pluginName": "BoyKisserAI",
                    "pluginDeveloper": "Developer",
                    "authenticationToken": "%s"
                  }
                }
                """.formatted(token);
        send(json);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("[VTS] Ошибка WebSocket: {}", error.getMessage());
    }

    public void send(String message) {
        if (webSocket != null) {
            webSocket.sendText(message, true);
        }
    }
}