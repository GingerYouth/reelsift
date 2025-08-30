package main.java.sift.bots.enums;

import java.util.Optional;

/** Delete enum. */
public enum Delete {
    EXCLUDED("Удалить исключения"),
    MANDATORY("Удалить предпочтения"),
    DATE("Удалить дату"),
    TIME("Удалить время"),
    AI("Удалить AI-запрос"),
    SUBS("Удалить фильтр по субтитрам"),
    ALL("Удалить все фильтры"),
    BACK("🔙 Назад");

    private final String name;

    Delete(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<Delete> getEnumByString(final String string) {
        for (final Delete delete : Delete.values()) {
            if (delete.name.equals(string)) {
                return Optional.of(delete);
            }
        }
        return Optional.empty();
    }
}
