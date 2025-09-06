package main.java.sift.bots.enums;

import java.util.Optional;

/** Edit enum. */
public enum Edit {
    EXCLUDED("Изменить исключения"),
    MANDATORY("Изменить предпочтения"),
    DATE("Изменить дату"),
    TIME("Изменить время"),
    AI_PROMPT("Изменить AI-запрос"),
    BACK("🔙 Назад");

    private final String name;

    Edit(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<Edit> getEnumByString(final String string) {
        for (final Edit edit : values()) {
            if (edit.name.equals(string)) {
                return Optional.of(edit);
            }
        }
        return Optional.empty();
    }
}
