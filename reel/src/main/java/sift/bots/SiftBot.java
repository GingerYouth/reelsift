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
import main.java.sift.City;
import main.java.sift.PropertiesLoader;
import main.java.sift.Session;
import main.java.sift.bots.enums.Common;
import main.java.sift.bots.enums.Delete;
import main.java.sift.bots.enums.Edit;
import main.java.sift.bots.enums.Guide;
import main.java.sift.bots.enums.Trigger;
import main.java.sift.bots.enums.UserState;
import main.java.sift.cache.RedisCache;
import main.java.sift.filters.DateInterval;
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
    "PMD.CouplingBetweenObjects",
    "PMD.ExcessiveImports"
})
public class SiftBot implements LongPollingSingleThreadUpdateConsumer {

    private final RedisCache redisCache = new RedisCache("localhost", 6379); // Use your Redis host/port
    private final TelegramClient telegramClient = new OkHttpTelegramClient(PropertiesLoader.get("tgApiKey"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");

    private final Map<Long, City> userCities = new ConcurrentHashMap<>();
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, DateInterval> dateFilters = new ConcurrentHashMap<>();
    private final Map<Long, String> timeFilters = new ConcurrentHashMap<>();
    private final Map<Long, Set<Genre>> excludedGenres = new ConcurrentHashMap<>();
    private final Map<Long, Set<Genre>> mandatoryGenres = new ConcurrentHashMap<>();
    private final Map<Long, String> aiPrompts = new ConcurrentHashMap<>();
    private final Set<Long> subFilters = ConcurrentHashMap.newKeySet();


    @Override
    public void consume(final Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        final long chatId = update.getMessage().getChatId();
        final String chatIdString = String.valueOf(chatId);
        final String text = update.getMessage().getText();
        if (!this.userCities.containsKey(chatId)) {
            this.userCities.put(chatId, City.MOSCOW);
            sendMessage(
                chatIdString,
                "👋 Привет! Добро пожаловать в SiftBot.\n"
                    + "По умолчанию установлен город Москва.\n"
                    + "Вы можете изменить город на Питер, написав в чат 'спб'.\n"
            );
            showMainKeyboard(chatIdString, "Выберите действие:");
            return;
        }
        if ("спб".equalsIgnoreCase(text)) {
            this.userCities.put(chatId, City.SPB);
            sendMessage(chatIdString, "Город изменен на Санкт-Петербург.");
            showMainKeyboard(chatIdString, "Выберите действие:");
            return;
        } else if ("мск".equalsIgnoreCase(text)) {
            this.userCities.put(chatId, City.MOSCOW);
            sendMessage(chatIdString, "Город изменен на Москва.");
            showMainKeyboard(chatIdString, "Выберите действие:");
            return;
        }

        Trigger.getEnumByString(text).ifPresentOrElse(
            trigger -> handleMainCommand(chatId, chatIdString, trigger),
            () -> Edit.getEnumByString(text).ifPresentOrElse(
                edit -> handleEditCommand(chatId, chatIdString, edit),
                    () -> Delete.getEnumByString(text).ifPresentOrElse(
                        delete -> handleDeleteCommand(chatId, chatIdString, delete),
                        () -> handleUserInput(chatId, chatIdString, text)
                    )
            )
        );
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private void handleMainCommand(final long chatId, final String chatIdStr, final Trigger command) {
        switch (command) {
            case Trigger.DATE:
                this.userStates.put(chatId, UserState.AWAITING_DATE);
                this.showMainKeyboard(chatIdStr, Guide.DATE.getName());
                break;

            case Trigger.EXCLUDED:
                this.userStates.put(chatId, UserState.AWAITING_EXCLUDED);
                this.showMainKeyboard(chatIdStr, Guide.EXCLUDED.getName());
                break;

            case Trigger.MANDATORY:
                this.userStates.put(chatId, UserState.AWAITING_MANDATORY);
                this.showMainKeyboard(chatIdStr, Guide.MANDATORY.getName());
                break;

            case Trigger.TIME:
                this.userStates.put(chatId, UserState.AWAITING_TIME);
                this.showMainKeyboard(chatIdStr, Guide.TIME.getName());
                break;

            case Trigger.AI:
                this.userStates.put(chatId, UserState.AWAITING_AI);
                this.showMainKeyboard(chatIdStr, Guide.AI.getName());
                break;

            case Trigger.SUBS_EN:
                this.subFilters.add(chatId);
                this.userStates.put(chatId, UserState.IDLE);
                break;

            case Trigger.SUBS_DIS:
                this.subFilters.remove(chatId);
                break;

            case Trigger.EDIT:
                this.userStates.put(chatId, UserState.EDITING_FILTERS);
                this.showEditMenu(chatIdStr);
                break;

            case Trigger.DELETE:
                this.userStates.put(chatId, UserState.DELETING_FILTERS);
                this.showDeleteMenu(chatIdStr);
                break;

            case Trigger.SEARCH:
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
                    final DateInterval dateInterval = validateDateInput(input);
                    this.dateFilters.put(chatId, dateInterval);
                    this.userStates.put(chatId, UserState.IDLE);
                    showMainKeyboard(chatIdStr, "✅ Диапазон дат сохранен: " + dateInterval);
                } catch (final IllegalArgumentException e) {
                    showMainKeyboard(chatIdStr, "❌ Неверный формат даты. " + Guide.DATE);
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

    private static DateInterval validateDateInput(final String input) {
        final String[] parts = input.split("-");
        if (parts.length == 1) {
            final LocalDate date = parseSingleDate(parts[0].trim());
            return new DateInterval(date, date);
        }
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid date format!");
        }
        final LocalDate start = parseSingleDate(parts[0].trim());
        final LocalDate end = parseSingleDate(parts[1].trim());
        return new DateInterval(start, end);
    }

    private static LocalDate parseSingleDate(final String dateStr) {
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

    private void handleEditCommand(final long chatId, final String chatIdStr, final Edit command) {
        switch (command) {
            case Edit.EXCLUDED:
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
                        excludedAsStr.isEmpty() ? Common.NOT_SET_UP.getName() : excludedAsStr,
                        Guide.EXCLUDED
                    )
                );
                break;

            case Edit.MANDATORY:
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
                        mandatoryAsStr.isEmpty() ? Common.NOT_SET_UP.getName() : mandatoryAsStr,
                        Guide.MANDATORY
                    )
                );
                break;

            case Edit.TIME:
                this.userStates.put(chatId, UserState.AWAITING_TIME);
                final String currentTime = timeFilters.getOrDefault(chatId, Common.NOT_SET_UP.getName());
                showMainKeyboard(
                    chatIdStr,
                    String.format("Текущее время: %s\n\n%s", currentTime, Guide.TIME)
                );
                break;

            case Edit.AI:
                userStates.put(chatId, UserState.AWAITING_AI);
                final String currentPrompt = aiPrompts.getOrDefault(chatId, Common.NOT_SET_UP.getName());
                showMainKeyboard(
                    chatIdStr,
                    String.format("Текущий AI-запрос: %s\n\n%s", currentPrompt, Guide.AI)
                );
                break;

            case Edit.BACK:
                userStates.put(chatId, UserState.IDLE);
                showMainKeyboard(chatIdStr, "Возврат в главное меню");
                break;

            default:
                break;
        }
    }

    private void handleDeleteCommand(final long chatId, final String chatIdStr, final Delete command) {
        switch (command) {
            case Delete.EXCLUDED -> this.excludedGenres.remove(chatId);
            case Delete.MANDATORY -> this.mandatoryGenres.remove(chatId);
            case Delete.DATE -> this.dateFilters.remove(chatId);
            case Delete.TIME -> this.timeFilters.remove(chatId);
            case Delete.AI -> this.aiPrompts.remove(chatId);
            case Delete.SUBS -> this.subFilters.remove(chatId);
            case Delete.ALL -> {
                this.excludedGenres.remove(chatId);
                this.mandatoryGenres.remove(chatId);
                this.dateFilters.remove(chatId);
                this.aiPrompts.remove(chatId);
                this.subFilters.remove(chatId);
            }
            case Delete.BACK -> {
                userStates.put(chatId, UserState.IDLE);
                showMainKeyboard(chatIdStr, "Возврат в главное меню");
            }
            default -> {}
        }
    }

    private void sendMessage(final String chatId, final String message) {
        final SendMessage sendMessage = new SendMessage(chatId, message);
        sendMessage.setParseMode("HTML");
        try {
            if (!sendMessage.getText().isEmpty()) {
                this.telegramClient.execute(sendMessage);
            }
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
        row1.add(Trigger.TIME.getName());
        row1.add(Trigger.DATE.getName());

        final KeyboardRow row2 = new KeyboardRow();
        row2.add(Trigger.EXCLUDED.getName());
        row2.add(Trigger.MANDATORY.getName());

        final KeyboardRow row3 = new KeyboardRow();
        row3.add(Trigger.AI.getName());
        row3.add(Trigger.EDIT.getName());

        final KeyboardRow row4 = new KeyboardRow();
        row4.add(
            this.subFilters.contains(Long.valueOf(chatId))
                ? Trigger.SUBS_DIS.getName()
                : Trigger.SUBS_EN.getName()
        );
        row4.add(Trigger.DELETE.getName());

        final KeyboardRow row5 = new KeyboardRow();
        row5.add(Trigger.SEARCH.getName());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        sendMessage.setReplyMarkup(keyboardMarkup);
        this.sendMessage(sendMessage);
    }

    private void showEditMenu(final String chatIdStr) {
        final long chatId = Long.parseLong(chatIdStr);
        final StringBuilder builder = new StringBuilder(140);
        builder.append("⚙️ <b>Текущие фильтры:</b>\n")
            .append("\n📅 <b>Дата:</b> ")
            .append(this.dateFilters.containsKey(chatId) ? this.dateFilters.get(chatId) : "сегодня")
            .append("\n⏰ <b>Время:</b> ").append(this.timeFilters.getOrDefault(chatId, "не задано"))
            .append("\n🚫 <b>Исключения:</b> ")
            .append(Genre.toStringOrDefault(this.excludedGenres.get(chatId), Common.NOT_SET_UP.getName()))
            .append("\n✅ <b>Предпочтения:</b> ")
            .append(Genre.toStringOrDefault(this.mandatoryGenres.get(chatId), Common.NOT_SET_UP.getName()))
            .append("\n🤖 <b>AI-запрос:</b> ").append(this.aiPrompts.getOrDefault(chatId, "не задан"))
            .append("\n <b>Только фильмы с субтитрами: </b> ").append(this.subFilters.contains(chatId) ? "да" : "нет");

        final SendMessage message = new SendMessage(chatIdStr, builder.toString());
        message.setParseMode("HTML");

        final List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1: Date and Time
        final KeyboardRow row1 = new KeyboardRow();
        row1.add(Edit.DATE.getName());
        row1.add(Edit.TIME.getName());

        // Row 2: Genres
        final KeyboardRow row2 = new KeyboardRow();
        row2.add(Edit.EXCLUDED.getName());
        row2.add(Edit.MANDATORY.getName());

        // Row 3: AI and back
        final KeyboardRow row3 = new KeyboardRow();
        row3.add(Edit.AI.getName());
        row3.add(Edit.BACK.getName());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        message.setReplyMarkup(keyboardMarkup);
        this.sendMessage(message);
    }

    private void showDeleteMenu(final String chatIdStr) {
        final long chatId = Long.parseLong(chatIdStr);
        final StringBuilder builder = new StringBuilder(140);
        builder.append("⚙️ <b>Текущие фильтры:</b>\n")
                .append("\n📅 <b>Дата:</b> ")
                .append(this.dateFilters.containsKey(chatId) ? this.dateFilters.get(chatId) : "сегодня")
                .append("\n⏰ <b>Время:</b> ").append(this.timeFilters.getOrDefault(chatId, "не задано"))
                .append("\n🚫 <b>Исключения:</b> ")
                .append(Genre.toStringOrDefault(this.excludedGenres.get(chatId), Common.NOT_SET_UP.getName()))
                .append("\n✅ <b>Предпочтения:</b> ")
                .append(Genre.toStringOrDefault(this.mandatoryGenres.get(chatId), Common.NOT_SET_UP.getName()))
                .append("\n🤖 <b>AI-запрос:</b> ").append(this.aiPrompts.getOrDefault(chatId, "не задан"))
                .append("\n <b>Только фильмы с субтитрами: </b> ")
                .append(this.subFilters.contains(chatId) ? "да" : "нет");

        final SendMessage message = new SendMessage(chatIdStr, builder.toString());
        message.setParseMode("HTML");

        final List<KeyboardRow> keyboard = new ArrayList<>();

        // Row 1: Date and Time
        final KeyboardRow row1 = new KeyboardRow();
        row1.add(Delete.DATE.getName());
        row1.add(Delete.TIME.getName());

        // Row 2: Genres
        final KeyboardRow row2 = new KeyboardRow();
        row2.add(Delete.EXCLUDED.getName());
        row2.add(Delete.MANDATORY.getName());

        // Row 3: AI and subs
        final KeyboardRow row3 = new KeyboardRow();
        row3.add(Delete.AI.getName());
        row3.add(Delete.SUBS.getName());

        // Row 4: back
        final KeyboardRow row4 = new KeyboardRow();
        row4.add(Delete.BACK.getName());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        message.setReplyMarkup(keyboardMarkup);
        this.sendMessage(message);
    }

    private void startSearch(final String chatIdString, final long chatId) throws IOException {
        sendMessage(chatIdString, Guide.SEARCH.getName());
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

        final DateInterval dateInterval = this.dateFilters.get(chatId);
        final DateInterval requitedDateInterval = dateInterval != null
            ? dateInterval
            : new DateInterval(LocalDate.now(), LocalDate.now());
        final List<LocalDate> requiredDates = requitedDateInterval.getDatesInRange();
        final List<LocalDate> cachedDates = this.redisCache.getCachedDates(this.userCities.get(chatId));
        final List<LocalDate> missingDates = requiredDates.stream()
            .filter(d -> !cachedDates.contains(d))
            .toList();

        final AfishaParser parser = new AfishaParser(this.userCities.get(chatId));
        for (final LocalDate missing : missingDates) {
            // TODO:: Optimize by creating interval
            final Map<String, String> missingMap = parser.parseFilmsInDates(missing.format(DATE_FORMATTER));
            for (final Map.Entry<String, String> entry : missingMap.entrySet()) {
                final List<Session> missingSessions = parser.parseSchedule(entry.getValue());
                this.redisCache.cacheSessions(missingSessions, this.userCities.get(chatId));
            }
        }

        final List<Session> resultSessions = this.redisCache
            .getCachedSessions(requiredDates, this.userCities.get(chatId));
        final List<Session> filtered = filters.filter(resultSessions);

        sendMessage(chatIdString, String.format("\uD83C\uDFAC Найдено %s сеансов!", filtered.size()));

        for (final String split : Session.toSplitStrings(filtered)) {
            sendMessage(chatIdString, split);
        }
    }
}