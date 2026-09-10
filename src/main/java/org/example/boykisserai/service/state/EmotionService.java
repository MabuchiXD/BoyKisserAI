package org.example.boykisserai.service.state;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.boykisserai.domain.constant.EmotionType;
import org.example.boykisserai.service.animation.AnimationService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Getter
public class EmotionService {

    private final AnimationService animationService;
    private EmotionType currentEmotion = EmotionType.JOY;

    public EmotionService(AnimationService animationService) {
        this.animationService = animationService;
    }

    public void setEmotion(EmotionType newEmotion) {
        this.currentEmotion = newEmotion;
        log.info("[EmotionService] Эмоциональное состояние изменено на: {}", newEmotion);
        animationService.applyEmotion(newEmotion);
    }
}