package main.java.sift.bots.enums;

import java.util.Optional;

/** Trigger enum. */
public enum Trigger {
    DATE("📅 Задать дату"),
    TIME("⏰ Задать время начала сеанса"),
    EXCLUDED("🚫 Задать нежелательные жанры"),
    MANDATORY("✅ Задать предпочтительные жанры"),
    AI("🤖 Задать AI-запрос"),
    SUBS_EN("\uD83D\uDCAC Только фильмы с субтитрами"),
    SUBS_DIS("\uD83D\uDCAC Отключить фильтр по фильмам с субтитрами"),
    EDIT("⚙️ Изменить текущий фильтр"),
    DELETE("🗑️ Удалить текущий фильтр"),
    SEARCH("\uD83D\uDD0D Поиск!");

    private final String name;

    Trigger(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<Trigger> getEnumByString(final String string) {
        for (final Trigger trigger : Trigger.values()) {
            if (trigger.name.equals(string)) {
                return Optional.of(trigger);
            }
        }
        return Optional.empty();
    }
}
