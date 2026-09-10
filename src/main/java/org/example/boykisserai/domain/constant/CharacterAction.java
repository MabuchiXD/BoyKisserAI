package org.example.boykisserai.domain.constant;

public enum CharacterAction {
    NONE,   // Обычный диалог
    SLEEP,  // ИИ понял по смыслу, что ему приказали заткнуться/уйти в сон
    WAKE    // ИИ понял по смыслу, что ему разрешили говорить
}