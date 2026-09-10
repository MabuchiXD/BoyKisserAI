package org.example.boykisserai.domain.constant;

public enum RelationshipType {
    CREATOR("Создатель"),
    BELOVED("Любимчик"),
    FRIEND("Друг"),
    NEUTRAL("Знакомый"),
    SUSPICIOUS("Подозрительный"),
    HOSTILE("Враг");

    private final String title;

    RelationshipType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}