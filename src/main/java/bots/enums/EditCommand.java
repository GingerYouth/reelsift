package bots.enums;

import java.util.Optional;

/** Edit enum. */
public enum EditCommand implements Command{
    EXCLUDED("Изменить исключения"),
    MANDATORY("Изменить предпочтения"),
    DATE("Изменить дату"),
    TIME("Изменить время"),
    AI_PROMPT("Изменить AI-запрос"),
    BACK("🔙 Назад");

    private final String name;

    EditCommand(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<EditCommand> getEnumByString(final String string) {
        for (final EditCommand editCommand : values()) {
            if (editCommand.name.equals(string)) {
                return Optional.of(editCommand);
            }
        }
        return Optional.empty();
    }
}
