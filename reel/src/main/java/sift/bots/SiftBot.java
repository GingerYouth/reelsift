package main.java.sift.bots;

import main.java.sift.PropertiesLoader;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SiftBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient = new OkHttpTelegramClient(PropertiesLoader.get("tgApiKey"));

    private enum UserState {
        AWAITING_TIME,
        AWAITING_EXCLUDED,
        AWAITING_MANDATORY,
        AWAITING_AI,
        IDLE
    }

    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, String> excludedGenresStore = new ConcurrentHashMap<>();
    private final Map<Long, String> mandatoryGenresStore = new ConcurrentHashMap<>();

    private static final String TIME_TRIGGER = "Задать время начала сеанса";
    private static final String TIME_FILTER_GUIDE = "Укажите желаемый диапазон времени начала сеанса" +
        " в формате HH:MM-HH:MM, например 18:30-23:30";

    private static final String EXCLUDED_TRIGGER = "Задать нежелательные жанры";
    private static final String EXCLUDED_FILTER_GUIDE = "Укажите нежелательные жанры, например: Комедия, мелодрама";

    private static final String MANDATORY_TRIGGER = "Задать предпочтительные жанры";
    private static final String MANDATORY_GUIDE = "Укажите жанры для поиска, например: Триллер, драма\n";

    private static final String AI_TRIGGER = "Добавить AI-фильтр";
    private static final String AI_GUIDE = "Дайте искусственному интеллекту найти вам сеанс.\n" +
            "Для этого продолжите запрос: Я хочу сходить в кинотеатр и посмотреть...";

    private static final Set<String> TRIGGERS = Set.of(
        TIME_TRIGGER,
        EXCLUDED_TRIGGER,
        MANDATORY_TRIGGER,
        AI_TRIGGER
    );

    @Override
    public void consume(Update update) {
        final long chatId = update.getMessage().getChatId();
        final String chatIdString = String.valueOf(chatId);
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            this.sendCustomKeyboard(chatIdString, "Выберите действие из меню:");
            return;
        }
        final String text = update.getMessage().getText();
        if (TRIGGERS.contains(text)) {
            this.handleCommand(chatId, chatIdString, text);
        } else {
            this.handleUserInput(chatId, chatIdString, text);
        }
    }

    private void handleCommand(long chatId, String chatIdStr, String command) {
        switch (command) {
            case EXCLUDED_TRIGGER:
                userStates.put(chatId, UserState.AWAITING_EXCLUDED);
                sendCustomKeyboard(chatIdStr, EXCLUDED_FILTER_GUIDE);
                break;

            case MANDATORY_TRIGGER:
                userStates.put(chatId, UserState.AWAITING_MANDATORY);
                sendCustomKeyboard(chatIdStr, MANDATORY_GUIDE);
                break;

            case TIME_TRIGGER:
                userStates.put(chatId, UserState.AWAITING_TIME);
                sendCustomKeyboard(chatIdStr, TIME_FILTER_GUIDE);
                break;

            case AI_TRIGGER:
                userStates.put(chatId, UserState.AWAITING_AI);
                sendCustomKeyboard(chatIdStr, AI_GUIDE);
                break;

//            case SEARCH_COMMAND:
//                userStates.put(chatId, UserState.IDLE);
//                executeSearch(chatIdStr);
//                break;
//
//            case EDIT_COMMAND:
//                userStates.put(chatId, UserState.IDLE);
//                showCurrentFilters(chatIdStr);
//                break;

            default:
                sendCustomKeyboard(chatIdStr, "Выберите действие: ");
        }
    }

    private void handleUserInput(long chatId, String chatIdStr, String input) {
        UserState state = userStates.getOrDefault(chatId, UserState.IDLE);

        switch (state) {
            case AWAITING_EXCLUDED:
                this.excludedGenresStore.put(chatId, input);
                this.userStates.put(chatId, UserState.IDLE);
                this.sendCustomKeyboard(chatIdStr, "✅ Нежелательные жанры сохранены: " + input);
                break;

            case AWAITING_MANDATORY:
                this.mandatoryGenresStore.put(chatId, input);
                this.userStates.put(chatId, UserState.IDLE);
                this.sendCustomKeyboard(chatIdStr, "✅ Обязательные жанры сохранены: " + input);
                break;

            case AWAITING_TIME:
                this.userStates.put(chatId, UserState.IDLE);
                this.sendCustomKeyboard(chatIdStr, "✅ Временной диапазон сохранен: " + input);
                break;

            case AWAITING_AI:
                this.userStates.put(chatId, UserState.IDLE);
                this.sendCustomKeyboard(chatIdStr, "✅ AI-запрос принят: " + input);
                break;

            default:
                sendCustomKeyboard(chatIdStr, "Выберите действие из меню:");
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

    public void sendCustomKeyboard(final String chatId, final String message) {
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
        row.add("Изменить текущие фильтры");
        row.add("Поиск!");
        keyboard.add(row);
        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        sendMessage.setReplyMarkup(keyboardMarkup);
        this.sendMessage(sendMessage);
    }
}