package org.example.boykisserai.domain.constant;

public enum BehaviorType {
    SILENT,   // Молчать, пока пользователь сам не обратится
    QUIET,    // Отвечать максимально коротко
    NORMAL,   // Обычный режим
    CHATTY,   // Разговорчивый (больше общения)
    BUSY,     // Занят
    SLEEPING  // Спит
}