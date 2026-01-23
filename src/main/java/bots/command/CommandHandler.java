package bots.command;

import bots.enums.Common;
import bots.enums.Delete;
import bots.enums.Edit;
import bots.enums.Guide;
import bots.enums.Trigger;
import bots.enums.UserState;
import bots.messaging.MessageSender;
import bots.state.UserStateManager;
import bots.ui.KeyboardFactory;
import filters.Genre;

import java.util.stream.Collectors;

/**
 * Handles command routing and execution.
 * Routes different command types to appropriate handlers.
 */
public class CommandHandler {

    private final UserStateManager stateManager;
    private final MessageSender messageSender;
    private final KeyboardFactory keyboardFactory;
    private final SearchCommandHandler searchHandler;

    public CommandHandler(
            final UserStateManager stateManager,
            final MessageSender messageSender,
            final KeyboardFactory keyboardFactory,
            final SearchCommandHandler searchHandler) {
        this.stateManager = stateManager;
        this.messageSender = messageSender;
        this.keyboardFactory = keyboardFactory;
        this.searchHandler = searchHandler;
    }

    /**
     * Handles main trigger commands.
     *
     * @param chatId the chat ID
     * @param chatIdStr the chat ID as string
     * @param command the trigger command
     */
    public void handleTrigger(final long chatId, final String chatIdStr, final Trigger command) {
        switch (command) {
            case DATE -> handleDateTrigger(chatId, chatIdStr);
            case EXCLUDED -> handleExcludedTrigger(chatId, chatIdStr);
            case MANDATORY -> handleMandatoryTrigger(chatId, chatIdStr);
            case TIME -> handleTimeTrigger(chatId, chatIdStr);
            case AI_PROMPT -> handleAiPromptTrigger(chatId, chatIdStr);
            case SUBS_EN -> handleSubsEnableTrigger(chatId);
            case SUBS_DIS -> handleSubsDisableTrigger(chatId);
            case EDIT -> handleEditTrigger(chatId, chatIdStr);
            case DELETE -> handleDeleteTrigger(chatId, chatIdStr);
            case SEARCH -> handleSearchTrigger(chatId, chatIdStr);
            default -> showMainMenu(chatIdStr, "Выберите действие: ");
        }
    }

    /**
     * Handles edit commands.
     *
     * @param chatId the chat ID
     * @param chatIdStr the chat ID as string
     * @param command the edit command
     */
    public void handleEdit(final long chatId, final String chatIdStr, final Edit command) {
        switch (command) {
            case EXCLUDED -> showEditExcludedGenres(chatId, chatIdStr);
            case MANDATORY -> showEditMandatoryGenres(chatId, chatIdStr);
            case TIME -> showEditTime(chatId, chatIdStr);
            case DATE -> showEditDate(chatId, chatIdStr);
            case AI_PROMPT -> showEditAiPrompt(chatId, chatIdStr);
            case BACK -> returnToMainMenu(chatId, chatIdStr);
            default -> {
                // No action needed
            }
        }
    }

    /**
     * Handles delete commands.
     *
     * @param chatId the chat ID
     * @param chatIdStr the chat ID as string
     * @param command the delete command
     */
    public void handleDelete(final long chatId, final String chatIdStr, final Delete command) {
        switch (command) {
            case EXCLUDED -> this.stateManager.removeExcludedGenres(chatId);
            case MANDATORY -> this.stateManager.removeMandatoryGenres(chatId);
            case DATE -> this.stateManager.removeDateFilter(chatId);
            case TIME -> this.stateManager.removeTimeFilter(chatId);
            case AI_PROMPT -> this.stateManager.removeAiPrompt(chatId);
            case SUBS -> this.stateManager.disableSubtitleFilter(chatId);
            case ALL -> this.stateManager.clearAllFilters(chatId);
            case BACK -> returnToMainMenu(chatId, chatIdStr);
            default -> {
                // No action needed
            }
        }
    }

    private void handleDateTrigger(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_DATE);
        showMainMenu(chatIdStr, Guide.DATE.getName());
    }

    private void handleExcludedTrigger(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_EXCLUDED);
        showMainMenu(chatIdStr, Guide.EXCLUDED.getName());
    }

    private void handleMandatoryTrigger(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_MANDATORY);
        showMainMenu(chatIdStr, Guide.MANDATORY.getName());
    }

    private void handleTimeTrigger(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_TIME);
        showMainMenu(chatIdStr, Guide.TIME.getName());
    }

    private void handleAiPromptTrigger(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_AI);
        showMainMenu(chatIdStr, Guide.AI_PROMPT.getName());
    }

    private void handleSubsEnableTrigger(final long chatId) {
        this.stateManager.enableSubtitleFilter(chatId);
        this.stateManager.setState(chatId, UserState.IDLE);
    }

    private void handleSubsDisableTrigger(final long chatId) {
        this.stateManager.disableSubtitleFilter(chatId);
    }

    private void handleEditTrigger(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.EDITING_FILTERS);
        final var message = this.keyboardFactory.createEditMenuMessage(chatIdStr);
        this.messageSender.send(message);
    }

    private void handleDeleteTrigger(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.DELETING_FILTERS);
        final var message = this.keyboardFactory.createDeleteMenuMessage(chatIdStr);
        this.messageSender.send(message);
    }

    private void handleSearchTrigger(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.SEARCHING);
        this.searchHandler.executeSearch(chatId, chatIdStr);
    }

    private void showEditExcludedGenres(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_EXCLUDED);
        final String excludedAsStr = this.stateManager.getExcludedGenres(chatId)
                .stream()
                .map(Genre::name)
                .collect(Collectors.joining(", "));
        showMainMenu(
                chatIdStr,
                String.format(
                        "Текущие исключения: %s\n\n%s",
                        excludedAsStr.isEmpty() ? Common.NOT_SET_UP.getName() : excludedAsStr,
                        Guide.EXCLUDED
                )
        );
    }

    private void showEditMandatoryGenres(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_MANDATORY);
        final String mandatoryAsStr = this.stateManager.getMandatoryGenres(chatId)
                .stream()
                .map(Genre::name)
                .collect(Collectors.joining(", "));
        showMainMenu(
                chatIdStr,
                String.format(
                        "Текущие предпочтения: %s\n\n%s",
                        mandatoryAsStr.isEmpty() ? Common.NOT_SET_UP.getName() : mandatoryAsStr,
                        Guide.MANDATORY
                )
        );
    }

    private void showEditTime(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_TIME);
        final String currentTime = this.stateManager.getTimeFilter(chatId) != null
                ? this.stateManager.getTimeFilter(chatId)
                : Common.NOT_SET_UP.getName();
        showMainMenu(
                chatIdStr,
                String.format("Текущее время: %s\n\n%s", currentTime, Guide.TIME)
        );
    }

    private void showEditDate(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_DATE);
        showMainMenu(chatIdStr, Guide.DATE.getName());
    }

    private void showEditAiPrompt(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.AWAITING_AI);
        final String currentPrompt = this.stateManager.getAiPrompt(chatId) != null
                ? this.stateManager.getAiPrompt(chatId)
                : Common.NOT_SET_UP.getName();
        showMainMenu(
                chatIdStr,
                String.format("Текущий AI-запрос: %s\n\n%s", currentPrompt, Guide.AI_PROMPT)
        );
    }

    private void returnToMainMenu(final long chatId, final String chatIdStr) {
        this.stateManager.setState(chatId, UserState.IDLE);
        showMainMenu(chatIdStr, "Возврат в главное меню");
    }

    private void showMainMenu(final String chatId, final String message) {
        final var sendMessage = this.keyboardFactory.createMainMenuMessage(chatId, message);
        this.messageSender.send(sendMessage);
    }
}
