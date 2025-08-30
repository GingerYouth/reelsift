package main.java.sift.bots;

public enum Delete {
    EXCLUDED("Удалить исключения"),
    MANDATORY("Удалить предпочтения"),
    DATE("Удалить дату"),
    TIME("Удалить время"),
    AI("Удалить AI-запрос"),
    SUBS("Удалить фильтр по субтитрам"),
    ALL("Удалить все фильтры");

    Delete(final String label) {}
}
