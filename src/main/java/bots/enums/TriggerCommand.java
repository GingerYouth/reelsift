package bots.enums;

import java.util.Optional;

/** Trigger enum. */
public enum TriggerCommand implements Command {
    DATE("📅 Задать дату"),
    TIME("⏰ Задать время начала сеанса"),
    EXCLUDED("🚫 Задать нежелательные жанры"),
    MANDATORY("✅ Задать предпочтительные жанры"),
    AI_PROMPT("🤖 Задать AI-запрос"),
    SUBS_EN("\uD83D\uDCAC Только фильмы с субтитрами"),
    SUBS_DIS("\uD83D\uDCAC Отключить фильтр по фильмам с субтитрами"),
    EDIT("⚙️ Изменить текущий фильтр"),
    DELETE("🗑️ Удалить текущий фильтр"),
    SEARCH("\uD83D\uDD0D Поиск!");

    private final String name;

    TriggerCommand(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<TriggerCommand> getEnumByString(final String string) {
        for (final TriggerCommand triggerCommand : values()) {
            if (triggerCommand.name.equals(string)) {
                return Optional.of(triggerCommand);
            }
        }
        return Optional.empty();
    }
}
