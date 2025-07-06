package main.java.sift.bots;

import java.io.IOException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import main.java.sift.AfishaParser;
import main.java.sift.PropertiesLoader;
import main.java.sift.Session;
import main.java.sift.filters.Filters;
import main.java.sift.filters.Genre;
import main.java.sift.filters.LlmFilter;
import main.java.sift.filters.MandatoryGenres;
import main.java.sift.filters.TimeFilter;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Telegram bot logic. */
@SuppressWarnings({
    "PMD.AvoidThrowingRawExceptionTypes",
    "PMD.ConsecutiveLiteralAppends",
    "PMD.TooManyMethods", "PMD.GodClass",
    "PMD.CouplingBetweenObjects"
})
public class SiftBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient = new OkHttpTelegramClient(PropertiesLoader.get("tgApiKey"));

    private enum UserState {
        AWAITING_DATE,
        AWAITING_TIME,
        AWAITING_EXCLUDED,
        AWAITING_MANDATORY,
        AWAITING_AI,
        EDITING_FILTERS,
        SEARCHING,
        IDLE
    }

    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, String> dateFilters = new ConcurrentHashMap<>();
    private final Map<Long, String> timeFilters = new ConcurrentHashMap<>();
    private final Map<Long, Set<Genre>> excludedGenres = new ConcurrentHashMap<>();
    private final Map<Long, Set<Genre>> mandatoryGenres = new ConcurrentHashMap<>();
    private final Map<Long, String> aiPrompts = new ConcurrentHashMap<>();

    private static final String TIME_TRIGGER = "⏰ Задать время начала сеанса";
    private static final String TIME_FILTER_GUIDE = "Укажите желаемый диапазон времени начала сеанса"
        + " в формате HH:MM-HH:MM, например 18:30-23:30";

    private static final String EXCLUDED_TRIGGER = "🚫 Задать нежелательные жанры";
    private static final String EXCLUDED_FILTER_GUIDE = "Укажите нежелательные жанры, например: Комедия, мелодрама";

    private static final String MANDATORY_TRIGGER = "✅ Задать предпочтительные жанры";
    private static final String MANDATORY_GUIDE = "Укажите жанры для поиска, например: Триллер, драма\n";

    private static final String AI_TRIGGER = "🤖 Добавить AI-фильтр";
    private static final String AI_GUIDE = "Добавить фильтрацию с помощью запроса к искусственному интеллекту.\n"
        + "Для этого продолжите запрос: Я хочу сходить в кинотеатр и посмотреть...";

    private static final String EDIT_TRIGGER = "⚙️ Изменить текущие фильтры";

    private static final String SEARCH_TRIGGER = "\uD83D\uDD0D Поиск!";
    private static final String SEARCH_GUIDE = "Пожалуйста, ожидайте...\n";

    private static final String DATE_TRIGGER = "📅 Задать дату";
    private static final String DATE_GUIDE = "Укажите дату в формате ДД.ММ либо диапазон дат в формате ДД.ММ-ДД.ММ\n"
        + "По умолчанию используется сегодняшняя дата.";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

    private static final String EDIT_DATE = "Изменить дату";
    private static final String EDIT_TIME = "Изменить время";
    private static final String EDIT_EXCLUDED = "Изменить исключения";
    private static final String EDIT_MANDATORY = "Изменить предпочтения";
    private static final String EDIT_AI = "Изменить AI-запрос";
    private static final String BACK_COMMAND = "🔙 Назад";

    public static final String NOT_SET_UP = "не заданы";

    private static final Set<String> TRIGGERS = Set.of(
        DATE_TRIGGER, TIME_TRIGGER, EXCLUDED_TRIGGER, MANDATORY_TRIGGER,
        AI_TRIGGER, EDIT_TRIGGER, SEARCH_TRIGGER
    );

    private static final Set<String> EDIT_COMMANDS = Set.of(
        EDIT_DATE, EDIT_TIME, EDIT_EXCLUDED, EDIT_MANDATORY, EDIT_AI, BACK_COMMAND
    );

    @Override
    public void consume(final Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        final long chatId = update.getMessage().getChatId();
        final String chatIdString = String.valueOf(chatId);
        final String text = update.getMessage().getText();
        if (TRIGGERS.contains(text)) {
            handleMainCommand(chatId, chatIdString, text);
        } else if (EDIT_COMMANDS.contains(text)) {
            handleEditCommand(chatId, chatIdString, text);
        } else {
            handleUserInput(chatId, chatIdString, text);
        }
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private void handleMainCommand(final long chatId, final String chatIdStr, final String command) {
        switch (command) {
            case DATE_TRIGGER:
                this.userStates.put(chatId, UserState.AWAITING_DATE);
                this.showMainKeyboard(chatIdStr, DATE_GUIDE);
                break;

            case EXCLUDED_TRIGGER:
                this.userStates.put(chatId, UserState.AWAITING_EXCLUDED);
                this.showMainKeyboard(chatIdStr, EXCLUDED_FILTER_GUIDE);
                break;

            case MANDATORY_TRIGGER:
                this.userStates.put(chatId, UserState.AWAITING_MANDATORY);
                this.showMainKeyboard(chatIdStr, MANDATORY_GUIDE);
                break;

            case TIME_TRIGGER:
                this.userStates.put(chatId, UserState.AWAITING_TIME);
                this.showMainKeyboard(chatIdStr, TIME_FILTER_GUIDE);
                break;

            case AI_TRIGGER:
                this.userStates.put(chatId, UserState.AWAITING_AI);
                this.showMainKeyboard(chatIdStr, AI_GUIDE);
                break;

            case EDIT_TRIGGER:
                this.userStates.put(chatId, UserState.EDITING_FILTERS);
                this.showEditMenu(chatIdStr);
                break;

            case SEARCH_TRIGGER:
                this.userStates.put(chatId, UserState.SEARCHING);
                try {
                    this.startSearch(chatIdStr, chatId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;

            default:
                showMainKeyboard(chatIdStr, "Выберите действие: ");
        }
    }

    private void handleUserInput(final long chatId, final String chatIdStr, final String input) {
        final UserState state = userStates.getOrDefault(chatId, UserState.IDLE);

        switch (state) {
            case AWAITING_DATE:
                try {
                    final String validated = validateDateInput(input);
                    this.dateFilters.put(chatId, validated);
                    this.userStates.put(chatId, UserState.IDLE);
                    showMainKeyboard(chatIdStr, "✅ Диапазон дат сохранен: " + validated);
                } catch (final IllegalArgumentException e) {
                    showMainKeyboard(chatIdStr, "❌ Неверный формат даты. " + DATE_GUIDE);
                }
                break;

            case AWAITING_EXCLUDED:
                final Genre.ParseResult excludedResult = Genre.parseGenres(input);
                if (excludedResult.invalidGenres().isEmpty()) {
                    this.excludedGenres.put(chatId, excludedResult.validGenres());
                    this.userStates.put(chatId, UserState.IDLE);
                    final String display = Genre.toStringOrDefault(excludedResult.validGenres(), "");
                    showMainKeyboard(chatIdStr, "✅ Нежелательные жанры сохранены: " + display);
                } else {
                    final String errorMessage = createGenreErrorMessage(excludedResult.invalidGenres());
                    showMainKeyboard(chatIdStr, errorMessage);
                }
                break;

            case AWAITING_MANDATORY:
                final Genre.ParseResult mandatoryResult = Genre.parseGenres(input);
                if (mandatoryResult.invalidGenres().isEmpty()) {
                    this.mandatoryGenres.put(chatId, mandatoryResult.validGenres());
                    this.userStates.put(chatId, UserState.IDLE);
                    final String display = Genre.toStringOrDefault(mandatoryResult.validGenres(), "");
                    showMainKeyboard(chatIdStr, "✅ Обязательные жанры сохранены: " + display);
                } else {
                    final String errorMessage = createGenreErrorMessage(mandatoryResult.invalidGenres());
                    showMainKeyboard(chatIdStr, errorMessage);
                }
                break;

            case AWAITING_TIME:
                this.timeFilters.put(chatId, input);
                this.userStates.put(chatId, UserState.IDLE);
                this.showMainKeyboard(chatIdStr, "✅ Временной диапазон сохранен: " + input);
                break;

            case AWAITING_AI:
                this.aiPrompts.put(chatId, input);
                this.userStates.put(chatId, UserState.IDLE);
                this.showMainKeyboard(chatIdStr, "✅ AI-запрос принят: " + input);
                break;

            default:
                showMainKeyboard(chatIdStr, "Выберите действие из меню:");
        }
    }

    private String validateDateInput(final String input) {
        final String[] parts = input.split("-");
        if (parts.length == 1) {
            final LocalDate start = parseSingleDate(parts[0].trim());
            return String.format("%s-%s", start, start);
        }
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid date format!");
        }
        final LocalDate start = parseSingleDate(parts[0].trim());
        final LocalDate end = parseSingleDate(parts[1].trim());
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date before start date!");
        }
        return input;
    }

    private LocalDate parseSingleDate(final String dateStr) {
        final LocalDate date = MonthDay.parse(dateStr, DATE_FORMATTER).atYear(Year.now().getValue());
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Invalid date format");
        }
        return date;
    }

    private String createGenreErrorMessage(final Set<String> invalidGenres) {
        final StringBuilder builder = new StringBuilder(50);
        builder.append("❌ Не распознаны жанры: ")
            .append(String.join(", ", invalidGenres))
            .append("\n\n\uD83D\uDCCB Доступные жанры:\n");

        // Group genres in columns for better readability
        final List<String> allGenres = new ArrayList<>(Genre.getDisplayNames());
        Collections.sort(allGenres);

        final int mid = (allGenres.size() + 1) / 2;
        for (int i = 0; i < mid; i++) {
            final String left = allGenres.get(i);
            final String right = (i + mid < allGenres.size()) ? allGenres.get(i + mid) : "";
            builder.append(String.format("%-20s %s%n", left, right));
        }

        return builder.toString();
    }

    private void handleEditCommand(final long chatId, final String chatIdStr, final String command) {
        switch (command) {
            case EDIT_EXCLUDED:
                this.userStates.put(chatId, UserState.AWAITING_EXCLUDED);
                final String excludedAsStr = this.excludedGenres
                    .getOrDefault(chatId, Collections.emptySet())
                    .stream()
                    .map(Genre::name)
                    .collect(Collectors.joining(", "));
                showMainKeyboard(
                    chatIdStr,
                    String.format(
                        "Текущие исключения: %s\n\n%s",
                        excludedAsStr.isEmpty() ? NOT_SET_UP : excludedAsStr,
                        EXCLUDED_FILTER_GUIDE
                    )
                );
                break;

            case EDIT_MANDATORY:
                this.userStates.put(chatId, UserState.AWAITING_MANDATORY);
                final String mandatoryAsStr = this.mandatoryGenres
                    .getOrDefault(chatId, Collections.emptySet())
                    .stream()
                    .map(Genre::name)
                    .collect(Collectors.joining(", "));
                showMainKeyboard(
                    chatIdStr,
                    String.format(
                        "Текущие предпочтения: %s\n\n%s",
                        mandatoryAsStr.isEmpty() ? NOT_SET_UP : mandatoryAsStr,
                        MANDATORY_GUIDE
                    )
                );
                break;

            case EDIT_TIME:
                this.userStates.put(chatId, UserState.AWAITING_TIME);
                final String currentTime = timeFilters.getOrDefault(chatId, NOT_SET_UP);
                showMainKeyboard(
                    chatIdStr,
                    String.format("Текущее время: %s\n\n%s", currentTime, TIME_FILTER_GUIDE)
                );
                break;

            case EDIT_AI:
                userStates.put(chatId, UserState.AWAITING_AI);
                final String currentPrompt = aiPrompts.getOrDefault(chatId, NOT_SET_UP);
                showMainKeyboard(
                    chatIdStr,
                    String.format("Текущий AI-запрос: %s\n\n%s", currentPrompt, AI_GUIDE)
                );
                break;

            case BACK_COMMAND:
                userStates.put(chatId, UserState.IDLE);
                showMainKeyboard(chatIdStr, "Возврат в главное меню");
                break;

            default:
                break;
        }
    }

    private void sendMessage(final String chatId, final String message) {
        final SendMessage sendMessage = new SendMessage(chatId, message);
        sendMessage.setParseMode("HTML");
        try {
            this.telegramClient.execute(sendMessage);
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }

    private void sendMessage(final SendMessage message) {
        message.setParseMode("HTML");
        try {
            this.telegramClient.execute(message);
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }

    private void showMainKeyboard(final String chatId, final String message) {
        final SendMessage sendMessage = new SendMessage(chatId, message);
        final List<KeyboardRow> keyboard = new ArrayList<>();

        final KeyboardRow row1 = new KeyboardRow();
        row1.add(TIME_TRIGGER);
        row1.add(DATE_TRIGGER);

        final KeyboardRow row2 = new KeyboardRow();
        row2.add(EXCLUDED_TRIGGER);
        row2.add(MANDATORY_TRIGGER);

        final KeyboardRow row3 = new KeyboardRow();
        row3.add(AI_TRIGGER);
        row3.add(EDIT_TRIGGER);

        final KeyboardRow row4 = new KeyboardRow();
        row4.add(SEARCH_TRIGGER);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        sendMessage.setReplyMarkup(keyboardMarkup);
        this.sendMessage(sendMessage);
    }

    private void showEditMenu(final String chatIdStr) {
        final long chatId = Long.parseLong(chatIdStr);
        final StringBuilder builder = new StringBuilder(140);
        builder.append("⚙️ <b>Текущие фильтры:</b>\n")
            .append("\n📅 <b>Дата:</b> ")
            .append(this.dateFilters.containsKey(chatId) ? dateFilters.get(chatId) : "сегодня")
            .append("\n⏰ <b>Время:</b> ").append(this.timeFilters.getOrDefault(chatId, "не задано"))
            .append("\n🚫 <b>Исключения:</b> ")
            .append(Genre.toStringOrDefault(this.excludedGenres.get(chatId), NOT_SET_UP))
            .append("\n✅ <b>Предпочтения:</b> ")
            .append(Genre.toStringOrDefault(this.mandatoryGenres.get(chatId), NOT_SET_UP))
            .append("\n🤖 <b>AI-запрос:</b> ").append(this.aiPrompts.getOrDefault(chatId, "не задан"));

        final SendMessage message = new SendMessage(chatIdStr, builder.toString());
        message.setParseMode("HTML");

        final List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1: Date and Time
        final KeyboardRow row1 = new KeyboardRow();
        row1.add(EDIT_DATE);
        row1.add(EDIT_TIME);

        // Row 2: Genres
        final KeyboardRow row2 = new KeyboardRow();
        row2.add(EDIT_EXCLUDED);
        row2.add(EDIT_MANDATORY);

        // Row 3: AI
        final KeyboardRow row3 = new KeyboardRow();
        row3.add(EDIT_AI);
        row3.add(BACK_COMMAND);

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        message.setReplyMarkup(keyboardMarkup);
        this.sendMessage(message);
    }

    private void startSearch(final String chatIdString, final long chatId) throws IOException {
        sendMessage(chatIdString, SEARCH_GUIDE);
        final Filters filters = new Filters();
        if (this.timeFilters.containsKey(chatId)) {
            filters.addFilter(
                new TimeFilter(
                    this.timeFilters.get(chatId).split("-")[0],
                    this.timeFilters.get(chatId).split("-")[1]
                )
            );
        }
        if (this.mandatoryGenres.containsKey(chatId)) {
            filters.addFilter(
                new MandatoryGenres(
                    this.mandatoryGenres.get(chatId).stream()
                        .map(Genre::getDisplayName)
                        .collect(Collectors.toList())
                )
            );
        }
        if (this.excludedGenres.containsKey(chatId)) {
            filters.addFilter(
                new MandatoryGenres(
                    this.excludedGenres.get(chatId).stream()
                        .map(Genre::getDisplayName)
                        .collect(Collectors.toList())
                )
            );
        }
        if (this.aiPrompts.containsKey(chatId)) {
            final LlmFilter llmFilter = new LlmFilter(this.aiPrompts.get(chatId));
            filters.addFilter(llmFilter);
        }
        final AfishaParser parser = new AfishaParser();
        final String date = this.dateFilters.get(chatId);
        final Map<String, String> films = date != null
            ? AfishaParser.parseFilmsInDates(date)
            : AfishaParser.parseTodayFilms();
        final List<Session> sessions = new ArrayList<>();
        for (final Map.Entry<String, String> entry : films.entrySet()) {
            sessions.addAll(parser.parseSchedule(entry.getValue()));
        }
        final List<Session> filtered = filters.filter(sessions);
        sendMessage(chatIdString, String.format("\uD83C\uDFAC Найдено %s сеансов!", filtered.size()));

        for (final String split : Session.toSplitStrings(filtered)) {
            sendMessage(chatIdString, split);
        }
    }
}