package org.example.boykisserai.provider.speech.localSound;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.config.AppProperties;
import org.example.boykisserai.provider.speech.SpeechState;
import org.example.boykisserai.provider.speech.SpeechToTextService;
import org.example.boykisserai.service.CharacterService;
import org.example.boykisserai.service.state.DashboardStateService;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@Service
public class LocalMicListenerService {

    private final CharacterService characterService;
    private final SpeechToTextService speechToTextService;
    private final DashboardStateService dashboardStateService;
    private final AppProperties props;

    public LocalMicListenerService(CharacterService characterService,
                                   SpeechToTextService speechToTextService,
                                   DashboardStateService dashboardStateService,
                                   AppProperties props) {
        this.characterService = characterService;
        this.speechToTextService = speechToTextService;
        this.dashboardStateService = dashboardStateService;
        this.props = props;
    }

    @PostConstruct
    public void startListening() {
        Thread thread = new Thread(this::listenToMicrophoneLoop);
        thread.setDaemon(true);
        thread.start();
    }

    private void listenToMicrophoneLoop() {
        try {
            AudioFormat format = new AudioFormat(48000.0f, 16, 1, true, false);
            TargetDataLine line = getPhysicalMicLine(format);

            if (line == null) {
                log.error("[LocalMic Error] Физический микрофон не найден в системе!");
                return;
            }

            line.open(format);
            line.start();
            log.info("[LocalMic OK] Звуковой поток микрофона активен (Порог: {})", props.getAudio().getSpeechThreshold());

            byte[] buffer = new byte[4096];
            ByteArrayOutputStream speechBuffer = new ByteArrayOutputStream();
            Deque<byte[]> prerollQueue = new ArrayDeque<>();

            boolean isRecording = false;
            long lastSpeechTimestamp = 0;

            double micGain = props.getAudio().getMicGain();
            double speechThreshold = props.getAudio().getSpeechThreshold();
            long silenceTimeoutMs = props.getAudio().getSilenceTimeoutMs();
            int prerollBufferCount = props.getAudio().getPrerollBufferCount();

            while (true) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) continue;

                if (dashboardStateService.isMicMuted()) {
                    dashboardStateService.setCurrentMicVolume(0);
                    speechBuffer.reset();
                    prerollQueue.clear();
                    isRecording = false;
                    continue;
                }

                byte[] amplifiedChunk = applySoftwareGain(buffer, bytesRead, micGain);
                double volume = calculateVolume(amplifiedChunk, bytesRead);
                dashboardStateService.setCurrentMicVolume(volume);

                //Блок звука с микрофона, пока бот разговаривает
                if (SpeechState.isSpeaking) {
                    speechBuffer.reset();
                    prerollQueue.clear();
                    isRecording = false;
                    continue;
                }

                if (!isRecording) {
                    if (prerollQueue.size() >= prerollBufferCount) {
                        prerollQueue.pollFirst();
                    }
                    prerollQueue.addLast(amplifiedChunk);

                    if (volume > speechThreshold) {
                        isRecording = true;
                        lastSpeechTimestamp = System.currentTimeMillis();
                        log.debug("[LocalMic VAD] Старт записи речи (Громкость: {} > Порог: {})", (int) volume, (int) speechThreshold);

                        while (!prerollQueue.isEmpty()) {
                            byte[] preChunk = prerollQueue.pollFirst();
                            speechBuffer.write(preChunk, 0, preChunk.length);
                        }
                        speechBuffer.write(amplifiedChunk, 0, amplifiedChunk.length);
                    }
                } else {
                    speechBuffer.write(amplifiedChunk, 0, amplifiedChunk.length);

                    if (volume > speechThreshold) {
                        lastSpeechTimestamp = System.currentTimeMillis();
                    }

                    if (System.currentTimeMillis() - lastSpeechTimestamp > silenceTimeoutMs) {
                        byte[] audioData = speechBuffer.toByteArray();
                        speechBuffer.reset();
                        prerollQueue.clear();
                        isRecording = false;

                        if (audioData.length > 28000) {
                            log.info("[LocalMic VAD] Фраза завершена ({} байт). Отправка в STT...", audioData.length);
                            processSpeechAsync(audioData);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[LocalMic Error] Ошибка цикла микрофона: {}", e.getMessage());
        }
    }

    private byte[] applySoftwareGain(byte[] buffer, int length, double gain) {
        byte[] output = new byte[length];
        for (int i = 0; i < length; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xff));
            int amplified = (int) (sample * gain);

            if (amplified > Short.MAX_VALUE) amplified = Short.MAX_VALUE;
            else if (amplified < Short.MIN_VALUE) amplified = Short.MIN_VALUE;

            output[i] = (byte) (amplified & 0xff);
            output[i + 1] = (byte) ((amplified >> 8) & 0xff);
        }
        return output;
    }

    private void processSpeechAsync(byte[] audioData) {
        new Thread(() -> {
            String text = speechToTextService.recognizeSpeech(audioData);
            if (text != null && !text.isBlank()) {
                log.info("[LocalMic STT] Распознанный текст с микрофона: \"{}\"", text);
                characterService.chat("mabuchi", "Мабучи", "LOCAL", text);
            }
        }).start();
    }

    //У меня на компьютере два микрофона и виртуальный звуковой кабель, поэтому счёл полезным иметь детектор всех типов источников
    private TargetDataLine getPhysicalMicLine(AudioFormat format) throws Exception {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mixerInfo : mixers) {
            String name = mixerInfo.getName().toLowerCase();

            if (name.contains("primary") || name.contains("первичный") ||
                    name.contains("default") || name.contains("по умолчанию") ||
                    name.contains("cable") || name.contains("кабель") ||
                    name.contains("stereo mix") || name.contains("стерео микшер") ||
                    name.contains("voicemeeter") || name.contains("maudio")) {
                continue;
            }

            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(lineInfo)) {
                log.info("[LocalMic OK] Успешно привязан к устройству: {}", mixerInfo.getName());
                return (TargetDataLine) mixer.getLine(lineInfo);
            }
        }
        return null;
    }

    private double calculateVolume(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xff));
            sum += Math.abs(sample);
        }
        return (double) sum / (length / 2.0);
    }
}