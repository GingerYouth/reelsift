package main.java.sift.bots;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import main.java.sift.AfishaParser;
import main.java.sift.PropertiesLoader;
import main.java.sift.Session;
import main.java.sift.filters.Filters;
import main.java.sift.filters.LlmFilter;
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
@SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "PMD.ConsecutiveLiteralAppends"})
public class SiftBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient = new OkHttpTelegramClient(PropertiesLoader.get("tgApiKey"));

    private enum UserState {
        AWAITING_TIME,
        AWAITING_EXCLUDED,
        AWAITING_MANDATORY,
        AWAITING_AI,
        EDITING_FILTERS,
        SEARCHING,
        IDLE
    }

    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, String> timeFilters = new ConcurrentHashMap<>();
    private final Map<Long, String> excludedGenres = new ConcurrentHashMap<>();
    private final Map<Long, String> mandatoryGenres = new ConcurrentHashMap<>();
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
    private static final String EDIT_GUIDE = "Текущие фильтры. Для изменения введите номер фильтра.\n";

    private static final String SEARCH_TRIGGER = "\uD83D\uDD0D Поиск!";
    private static final String SEARCH_GUIDE = "Пожалуйста, ожидайте...\n";

    private static final String EDIT_TIME = "Изменить время";
    private static final String EDIT_EXCLUDED = "Изменить исключения";
    private static final String EDIT_MANDATORY = "Изменить предпочтения";
    private static final String EDIT_AI = "Изменить AI-запрос";
    private static final String BACK_COMMAND = "🔙 Назад";

    public static final String NOT_SET_UP = "не заданы";

    private static final Set<String> TRIGGERS = Set.of(
        TIME_TRIGGER, EXCLUDED_TRIGGER, MANDATORY_TRIGGER,
        AI_TRIGGER, EDIT_TRIGGER, SEARCH_TRIGGER
    );

    private static final Set<String> EDIT_COMMANDS = Set.of(
        EDIT_TIME, EDIT_EXCLUDED, EDIT_MANDATORY, EDIT_AI, BACK_COMMAND
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

    private void handleMainCommand(final long chatId, final String chatIdStr, final String command) {
        switch (command) {
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
            case AWAITING_EXCLUDED:
                this.excludedGenres.put(chatId, input);
                this.userStates.put(chatId, UserState.IDLE);
                this.showMainKeyboard(chatIdStr, "✅ Нежелательные жанры сохранены: " + input);
                break;

            case AWAITING_MANDATORY:
                this.mandatoryGenres.put(chatId, input);
                this.userStates.put(chatId, UserState.IDLE);
                this.showMainKeyboard(chatIdStr, "✅ Обязательные жанры сохранены: " + input);
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

    private void handleEditCommand(final long chatId, final String chatIdStr, final String command) {
        switch (command) {
            case EDIT_EXCLUDED:
                userStates.put(chatId, UserState.AWAITING_EXCLUDED);
                final String currentExcluded = excludedGenres.getOrDefault(chatId, NOT_SET_UP);
                showMainKeyboard(
                    chatIdStr,
                    String.format("Текущие исключения: %s\n\n%s", currentExcluded, EXCLUDED_FILTER_GUIDE)
                );
                break;

            case EDIT_MANDATORY:
                userStates.put(chatId, UserState.AWAITING_MANDATORY);
                final String currentMandatory = mandatoryGenres.getOrDefault(chatId, NOT_SET_UP);
                showMainKeyboard(
                    chatIdStr,
                    String.format("Текущие предпочтения: %s\n\n%s", currentMandatory, MANDATORY_GUIDE)
                );
                break;

            case EDIT_TIME:
                userStates.put(chatId, UserState.AWAITING_TIME);
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
        try {
            this.telegramClient.execute(sendMessage);
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }

    private void sendMessage(final SendMessage message) {
        try {
            this.telegramClient.execute(message);
        } catch (TelegramApiException tgApiEx) {
            tgApiEx.printStackTrace();
        }
    }

    public void showMainKeyboard(final String chatId, final String message) {
        final SendMessage sendMessage = new SendMessage(chatId, message);
        final List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        // TODO:: Add emojis
        row.add(TIME_TRIGGER);
        row.add(EXCLUDED_TRIGGER);
        row.add(MANDATORY_TRIGGER);
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(AI_TRIGGER);
        row.add(EDIT_TRIGGER);
        row.add(SEARCH_TRIGGER);
        keyboard.add(row);
        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        sendMessage.setReplyMarkup(keyboardMarkup);
        this.sendMessage(sendMessage);
    }

    private void showEditMenu(final String chatIdStr) {
        final long chatId = Long.parseLong(chatIdStr);
        final StringBuilder builder = new StringBuilder(120);
        builder.append("⚙️ <b>Текущие фильтры:</b>\n")
            .append("\n⏰ <b>Время:</b> ").append(this.timeFilters.getOrDefault(chatId, "не задано"))
            .append("\n🚫 <b>Исключения:</b> ").append(this.excludedGenres.getOrDefault(chatId, "не заданы"))
            .append("\n✅ <b>Предпочтения:</b> ").append(this.mandatoryGenres.getOrDefault(chatId, "не заданы"))
            .append("\n🤖 <b>AI-запрос:</b> ").append(this.aiPrompts.getOrDefault(chatId, "не задан"));

        final SendMessage message = new SendMessage(chatIdStr, builder.toString());
        message.setParseMode("HTML");

        final List<KeyboardRow> keyboard = new ArrayList<>();
        final KeyboardRow row1 = new KeyboardRow();
        row1.add(EDIT_TIME);
        row1.add(EDIT_EXCLUDED);

        final KeyboardRow row2 = new KeyboardRow();
        row2.add(EDIT_MANDATORY);
        row2.add(EDIT_AI);

        final KeyboardRow row3 = new KeyboardRow();
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

    // TODO:: Remove
    @SuppressWarnings("PMD.EmptyControlStatement")
    private void startSearch(final String chatIdString, final long chatId) throws IOException {
        final Filters filters = new Filters();
        if (this.timeFilters.containsKey(chatId)) {
            final TimeFilter timeFilter = new TimeFilter(
                this.timeFilters.get(chatId).split("-")[0],
                this.timeFilters.get(chatId).split("-")[1]
            );
            filters.addFilter(timeFilter);
        }
        if (this.mandatoryGenres.containsKey(chatId)) {
            // TODO::
        }
        if (this.excludedGenres.containsKey(chatId)) {
            // TODO::
        }
        if (this.aiPrompts.containsKey(chatId)) {
            final LlmFilter llmFilter = new LlmFilter(this.aiPrompts.get(chatId));
            filters.addFilter(llmFilter);
        }
        final AfishaParser parser = new AfishaParser();
        final Map<String, String> films = AfishaParser.parseTodayFilms();
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