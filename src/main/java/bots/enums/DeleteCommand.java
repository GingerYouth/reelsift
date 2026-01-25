package bots.enums;

import java.util.Optional;

/** Delete commands enum. */
public enum DeleteCommand implements Command {
    EXCLUDED("Удалить исключения"),
    MANDATORY("Удалить предпочтения"),
    DATE("Удалить дату"),
    TIME("Удалить время"),
    AI_PROMPT("Удалить AI-запрос"),
    SUBS("Удалить фильтр по субтитрам"),
    ALL("Удалить все фильтры"),
    BACK("🔙 Назад");

    private final String name;

    DeleteCommand(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<DeleteCommand> getEnumByString(final String string) {
        for (final DeleteCommand deleteCommand : values()) {
            if (deleteCommand.name.equals(string)) {
                return Optional.of(deleteCommand);
            }
        }
        return Optional.empty();
    }
}
