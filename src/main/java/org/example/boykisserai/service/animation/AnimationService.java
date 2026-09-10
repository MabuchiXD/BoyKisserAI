package org.example.boykisserai.service.animation;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.domain.constant.EmotionType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AnimationService {

    private final VtsParameterInjector injector;
    private volatile EmotionType currentActiveEmotion = EmotionType.JOY;
    private String currentActiveExp = null;

    private final List<String> allModelExpressions = List.of(
            "angry",
            "red eyes",
            "sadge",
            "boykisser face",
            "eye squint",
            "eye swap",
            "headpat ears",
            "arms toggle",
            "hair toggle",
            "long"
    );

    public AnimationService(VtsParameterInjector injector) {
        this.injector = injector;
    }


    //Анимация моргания, иначе оно не работает при использовании пресетов модельки в VTube Studio
    @PostConstruct
    public void startAutoBlinkLoop() {
        Thread blinkThread = new Thread(() -> {
            while (true) {
                try {
                    long nextBlinkDelay = 3500 + (long) (Math.random() * 1500);
                    Thread.sleep(nextBlinkDelay);

                    if (currentActiveEmotion == EmotionType.JOY || currentActiveEmotion == EmotionType.SURPRISE) {
                        continue;
                    }

                    performSmoothBlink();

                    if (Math.random() < 0.25) {
                        Thread.sleep(130);
                        performSmoothBlink();
                    }

                } catch (Exception ignored) {}
            }
        });
        blinkThread.setDaemon(true);
        blinkThread.start();
    }

    private double getBaseEyeOpen() {
        if (currentActiveEmotion == EmotionType.IRRITATION) return 0.40;
        if (currentActiveEmotion == EmotionType.SADNESS) return 0.45;
        if (currentActiveEmotion == EmotionType.FATIGUE) return 0.30;
        return 1.0;
    }

    private void performSmoothBlink() throws InterruptedException {
        double base = getBaseEyeOpen();

        setBlink(base * 0.70);
        Thread.sleep(30);
        setBlink(base * 0.30);
        Thread.sleep(30);

        setBlink(0.0);
        Thread.sleep(80);

        setBlink(base * 0.50);
        Thread.sleep(30);
        setBlink(base * 0.85);
        Thread.sleep(30);
        setBlink(base);
    }

    private void setBlink(double openState) {
        double inVal = openState * 0.5;
        injector.inject("EyeOpenLeft", inVal);
        injector.inject("EyeOpenRight", inVal);
    }

    public void setMouthOpen(double value) {
        injector.inject("MouthOpen", value);
        injector.inject("ParamMouthOpenY", value);
        injector.inject("VoiceVolumePlusMouthOpen", value);
    }

    public void setHeadTilt(double angle) {
        injector.inject("ParamAngleZ", angle);
        injector.inject("FaceAngleZ", angle);
    }

    public void setHeadAngleY(double angle) {
        injector.inject("ParamAngleY", angle);
        injector.inject("FaceAngleY", angle);
    }

    private void resetExpressions() {
        for (String exp : allModelExpressions) {
            injector.setExpression(exp, false);
        }
        injector.inject("ParamEyeBallX", 0.0);
        injector.inject("ParamEyeBallY", 0.0);
        injector.inject("ParamCheek", 0.0);
    }

    private void switchExpression(String newExp) {
        if (currentActiveExp != null && !currentActiveExp.equals(newExp)) {
            injector.setExpression(currentActiveExp, false);
        }
        if (newExp != null) {
            injector.setExpression(newExp, true);
        }
        this.currentActiveExp = newExp;
    }

    public void applyEmotion(EmotionType emotion) {
        if (emotion == null) return;
        this.currentActiveEmotion = emotion;
        log.info("[VTS Animation] Применяем выражение: {}", emotion);

        resetExpressions();
        setBlink(getBaseEyeOpen());

        switch (emotion) {
            case IRRITATION -> {
                switchExpression("angry");
                setHeadAngleY(-4.0);
                setHeadTilt(2.0);
            }
            case SADNESS -> {
                switchExpression("sadge");
                setHeadAngleY(-5.0);
                setHeadTilt(-2.0);
            }
            case AFFECTION -> {
                switchExpression("boykisser face");
                injector.inject("ParamCheek", 1.0);
                setHeadAngleY(-2.0);
                setHeadTilt(-5.0);
            }
            case JOY -> {
                switchExpression("eye squint");
                setHeadAngleY(2.0);
                setHeadTilt(0.0);
            }
            case SURPRISE -> {
                switchExpression("eye swap");
                setHeadAngleY(4.0);
                setMouthOpen(0.75);
                setHeadTilt(0.0);
            }
            case INTEREST -> {
                switchExpression("headpat ears");
                setHeadAngleY(2.0);
                setHeadTilt(6.0);
            }
            case FATIGUE -> {
                switchExpression("sadge");
                setHeadAngleY(-3.0);
                setHeadTilt(2.0);
            }
            case TRUST -> {
                switchExpression(null);
                setHeadAngleY(0.0);
                setHeadTilt(0.0);
            }
        }
    }


    @Async("vtuberAsyncExecutor")
    public void simulateSpeaking(long durationMs) {
        try {
            log.debug("[VTS LipSync] Шевеление ртом на {} мс...", durationMs);
            long endTime = System.currentTimeMillis() + durationMs;

            while (System.currentTimeMillis() < endTime) {
                double randomMouth = 0.30 + (Math.random() * 0.65);
                setMouthOpen(randomMouth);
                Thread.sleep(70 + (long) (Math.random() * 50));
            }

            setMouthOpen(0.0);
        } catch (InterruptedException e) {
            setMouthOpen(0.0);
        }
    }
}