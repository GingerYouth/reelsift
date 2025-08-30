package main.java.sift.bots.enums;

import java.util.Optional;

/** Guide enum. */
public enum Guide {
    TIME("Укажите желаемый диапазон времени начала сеанса"
        + " в формате HH:MM-HH:MM, например 18:30-23:30"
    ),
    EXCLUDED("Укажите нежелательные жанры, например: Комедия, мелодрама"),
    MANDATORY("Укажите жанры для поиска, например: Триллер, драма\n"),
    AI("Добавить фильтрацию с помощью запроса к искусственному интеллекту.\n"
        + "Для этого продолжите запрос: Я хочу сходить в кинотеатр и посмотреть..."
    ),
    SEARCH("Пожалуйста, ожидайте...\n"),
    DATE("Укажите дату в формате ДД.ММ либо диапазон дат в формате ДД.ММ-ДД.ММ\n"
        + "По умолчанию используется сегодняшняя дата."
    );

    private final String name;

    Guide(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public static Optional<String> getEnumByString(final String string) {
        for (final Guide guide : Guide.values()) {
            if (guide.name.equals(string)) {
                return Optional.of(guide.name());
            }
        }
        return Optional.empty();
    }
}
