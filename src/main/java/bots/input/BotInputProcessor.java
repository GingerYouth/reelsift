package bots.input;

import bots.enums.UserState;
import bots.exception.InvalidDateException;
import bots.messaging.MessageSender;
import bots.state.UserStateManager;
import bots.ui.KeyboardFactory;
import bots.validation.InputValidator;
import filters.DateInterval;
import filters.Genre;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Processes user input based on their current state.
 * Handles input validation and state transitions without nested switch statements.
 */
public class BotInputProcessor {

    private final UserStateManager stateManager;
    private final MessageSender messageSender;
    private final KeyboardFactory keyboardFactory;

    public BotInputProcessor(
            final UserStateManager stateManager,
            final MessageSender messageSender,
            final KeyboardFactory keyboardFactory) {
        this.stateManager = stateManager;
        this.messageSender = messageSender;
        this.keyboardFactory = keyboardFactory;
    }

    /**
     * Processes user input based on their current state.
     *
     * @param chatId the chat ID
     * @param chatIdStr the chat ID as string
     * @param input the user input
     */
    public void processInput(final long chatId, final String chatIdStr, final String input) {
        final UserState state = this.stateManager.getState(chatId);

        switch (state) {
            case AWAITING_DATE -> processDateInput(chatId, chatIdStr, input);
            case AWAITING_EXCLUDED -> processExcludedGenresInput(chatId, chatIdStr, input);
            case AWAITING_MANDATORY -> processMandatoryGenresInput(chatId, chatIdStr, input);
            case AWAITING_TIME -> processTimeInput(chatId, chatIdStr, input);
            case AWAITING_AI -> processAiPromptInput(chatId, chatIdStr, input);
            default -> showMainMenu(chatIdStr, "Выберите действие из меню:");
        }
    }

    private void processDateInput(final long chatId, final String chatIdStr, final String input) {
        try {
            final DateInterval dateInterval = InputValidator.validateDateInput(input);
            this.stateManager.setDateFilter(chatId, dateInterval);
            this.stateManager.setState(chatId, UserState.IDLE);
            showMainMenu(chatIdStr, "✅ Диапазон дат сохранен: " + dateInterval);
        } catch (final InvalidDateException e) {
            showMainMenu(chatIdStr, "❌ Неверный формат даты. " + e.getMessage());
        }
    }

    private void processExcludedGenresInput(final long chatId, final String chatIdStr, final String input) {
        final InputValidator.GenreValidationResult result = InputValidator.validateGenres(input);
        if (result.isValid()) {
            this.stateManager.setExcludedGenres(chatId, result.getValidGenres());
            this.stateManager.setState(chatId, UserState.IDLE);
            final String display = Genre.toStringOrDefault(result.getValidGenres(), "");
            showMainMenu(chatIdStr, "✅ Нежелательные жанры сохранены: " + display);
        } else {
            final String errorMessage = createGenreErrorMessage(result.getInvalidGenres());
            showMainMenu(chatIdStr, errorMessage);
        }
    }

    private void processMandatoryGenresInput(final long chatId, final String chatIdStr, final String input) {
        final InputValidator.GenreValidationResult result = InputValidator.validateGenres(input);
        if (result.isValid()) {
            this.stateManager.setMandatoryGenres(chatId, result.getValidGenres());
            this.stateManager.setState(chatId, UserState.IDLE);
            final String display = Genre.toStringOrDefault(result.getValidGenres(), "");
            showMainMenu(chatIdStr, "✅ Обязательные жанры сохранены: " + display);
        } else {
            final String errorMessage = createGenreErrorMessage(result.getInvalidGenres());
            showMainMenu(chatIdStr, errorMessage);
        }
    }

    private void processTimeInput(final long chatId, final String chatIdStr, final String input) {
        this.stateManager.setTimeFilter(chatId, input);
        this.stateManager.setState(chatId, UserState.IDLE);
        showMainMenu(chatIdStr, "✅ Временной диапазон сохранен: " + input);
    }

    private void processAiPromptInput(final long chatId, final String chatIdStr, final String input) {
        this.stateManager.setAiPrompt(chatId, input);
        this.stateManager.setState(chatId, UserState.IDLE);
        showMainMenu(chatIdStr, "✅ AI-запрос принят: " + input);
    }

    /**
     * Shows main menu with keyboard.
     *
     * @param chatId the chat ID
     * @param message the message to display
     */
    public void showMainMenu(final String chatId, final String message) {
        final var sendMessage = this.keyboardFactory.createMainMenuMessage(chatId, message);
        this.messageSender.send(sendMessage);
    }

    /**
     * Creates error message for invalid genres with list of valid genres.
     *
     * @param invalidGenres the set of invalid genre names
     * @return formatted error message
     */
    private static String createGenreErrorMessage(final Set<String> invalidGenres) {
        final StringBuilder builder = new StringBuilder(150);
        builder.append("❌ Не распознаны жанры: ")
                .append(String.join(", ", invalidGenres))
                .append("\n\n\uD83D\uDCCB Доступные жанры:\n");

        // Group genres in columns for better readability
        final var allGenres = Genre.getDisplayNames().stream()
                .sorted()
                .collect(Collectors.toList());

        final int mid = (allGenres.size() + 1) / 2;
        for (int i = 0; i < mid; i++) {
            final String left = allGenres.get(i);
            final String right = i + mid < allGenres.size() ? allGenres.get(i + mid) : "";
            builder.append(String.format("%-20s %s%n", left, right));
        }

        return builder.toString();
    }
}
