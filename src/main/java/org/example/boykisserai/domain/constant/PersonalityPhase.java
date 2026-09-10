package org.example.boykisserai.domain.constant;

public enum PersonalityPhase {
    CYNIC_MEME("Мемный циник (Дефолт)"),
    DEMON_TOXIC("Абсолютный Токсик / Демон"),
    CUTE_FEMBOY("Милый фембойчик"),
    OBEDIENT_PET("Послушный кот Создателя");

    private final String title;

    PersonalityPhase(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}