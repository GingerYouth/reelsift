package bots.services.handlers;

import bots.enums.Guide;
import bots.enums.UserState;
import bots.services.InputValidator;
import bots.services.KeyboardService;
import bots.services.UserService;
import filters.DateInterval;
import filters.Genre;

public class UserInputHandler {
    private final UserService userService;
    private final KeyboardService keyboardService;

    public UserInputHandler(final UserService userService, final KeyboardService keyboardService) {
        this.userService = userService;
        this.keyboardService = keyboardService;
    }

    public void handleUserInput(final long chatId, final String chatIdStr, final String input) {
        final UserState state = userService.getUserState(chatId);

        switch (state) {
            case AWAITING_DATE:
                handleDateInput(chatId, chatIdStr, input);
                break;

            case AWAITING_EXCLUDED:
                handleExcludedGenresInput(chatId, chatIdStr, input);
                break;

            case AWAITING_MANDATORY:
                handleMandatoryGenresInput(chatId, chatIdStr, input);
                break;

            case AWAITING_TIME:
                handleTimeInput(chatId, chatIdStr, input);
                break;

            case AWAITING_AI:
                handleAiInput(chatId, chatIdStr, input);
                break;

            default:
                keyboardService.showMainKeyboard(chatIdStr, "Выберите действие из меню:");
                break;
        }
    }

    private void handleDateInput(final long chatId, final String chatIdStr, final String input) {
        try {
            final DateInterval dateInterval = InputValidator.validateDateInput(input);
            this.userService.setDateFilter(chatId, dateInterval);
            this.userService.setUserState(chatId, UserState.IDLE);
            keyboardService.showMainKeyboard(chatIdStr, "✅ Диапазон дат сохранен: " + dateInterval);
        } catch (final IllegalArgumentException e) {
            keyboardService.showMainKeyboard(chatIdStr, "❌ Неверный формат даты. " + Guide.DATE);
        }
    }

    private void handleExcludedGenresInput(final long chatId, final String chatIdStr, final String input) {
        final Genre.ParseResult excludedResult = Genre.parseGenres(input);
        if (excludedResult.invalidGenres().isEmpty()) {
            this.userService.setExcludedGenres(chatId, excludedResult.validGenres());
            this.userService.setUserState(chatId, UserState.IDLE);
            final String display = Genre.toStringOrDefault(excludedResult.validGenres(), "");
            keyboardService.showMainKeyboard(chatIdStr, "✅ Нежелательные жанры сохранены: " + display);
        } else {
            final String errorMessage = InputValidator.createGenreErrorMessage(excludedResult.invalidGenres());
            keyboardService.showMainKeyboard(chatIdStr, errorMessage);
        }
    }

    private void handleMandatoryGenresInput(final long chatId, final String chatIdStr, final String input) {
        final Genre.ParseResult mandatoryResult = Genre.parseGenres(input);
        if (mandatoryResult.invalidGenres().isEmpty()) {
            this.userService.setMandatoryGenres(chatId, mandatoryResult.validGenres());
            this.userService.setUserState(chatId, UserState.IDLE);
            final String display = Genre.toStringOrDefault(mandatoryResult.validGenres(), "");
            keyboardService.showMainKeyboard(chatIdStr, "✅ Обязательные жанры сохранены: " + display);
        } else {
            final String errorMessage = InputValidator.createGenreErrorMessage(mandatoryResult.invalidGenres());
            keyboardService.showMainKeyboard(chatIdStr, errorMessage);
        }
    }

    private void handleTimeInput(final long chatId, final String chatIdStr, final String input) {
        this.userService.setTimeFilter(chatId, input);
        this.userService.setUserState(chatId, UserState.IDLE);
        this.keyboardService.showMainKeyboard(chatIdStr, "✅ Временной диапазон сохранен: " + input);
    }

    private void handleAiInput(final long chatId, final String chatIdStr, final String input) {
        this.userService.setAiPrompt(chatId, input);
        this.userService.setUserState(chatId, UserState.IDLE);
        this.keyboardService.showMainKeyboard(chatIdStr, "✅ AI-запрос принят: " + input);
    }
}