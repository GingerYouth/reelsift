package bots.ui;

import bots.enums.Common;
import bots.enums.Delete;
import bots.enums.Edit;
import bots.enums.Guide;
import bots.enums.Trigger;
import bots.state.UserStateManager;
import filters.Genre;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

/**
 * Factory for creating keyboard layouts and formatted messages.
 * Consolidates all UI rendering logic in one place to reduce StringBuilder concatenation issues.
 */
public class KeyboardFactory {

    private final UserStateManager stateManager;

    public KeyboardFactory(final UserStateManager stateManager) {
        this.stateManager = stateManager;
    }

    /**
     * Creates main menu message with keyboard.
     *
     * @param chatId the chat ID
     * @param message the message text
     * @return configured SendMessage
     */
    public SendMessage createMainMenuMessage(final String chatId, final String message) {
        final SendMessage sendMessage = new SendMessage(chatId, message);
        final List<KeyboardRow> keyboard = new ArrayList<>();

        addRow(keyboard, Trigger.TIME.getName(), Trigger.DATE.getName());
        addRow(keyboard, Trigger.EXCLUDED.getName(), Trigger.MANDATORY.getName());
        addRow(keyboard, Trigger.AI_PROMPT.getName(), Trigger.EDIT.getName());

        final long chatIdLong = Long.parseLong(chatId);
        final String subsButton = this.stateManager.hasSubtitleFilter(chatIdLong)
                ? Trigger.SUBS_DIS.getName()
                : Trigger.SUBS_EN.getName();
        addRow(keyboard, subsButton, Trigger.DELETE.getName());
        addRow(keyboard, Trigger.SEARCH.getName());

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        sendMessage.setReplyMarkup(keyboardMarkup);
        return sendMessage;
    }

    /**
     * Creates edit filters menu message.
     *
     * @param chatId the chat ID
     * @return configured SendMessage
     */
    public SendMessage createEditMenuMessage(final String chatId) {
        final long chatIdLong = Long.parseLong(chatId);
        final String filtersSummary = buildFiltersSummary(chatIdLong);
        final SendMessage message = new SendMessage(chatId, filtersSummary);

        final List<KeyboardRow> keyboard = new ArrayList<>();
        addRow(keyboard, Edit.DATE.getName(), Edit.TIME.getName());
        addRow(keyboard, Edit.EXCLUDED.getName(), Edit.MANDATORY.getName());
        addRow(keyboard, Edit.AI_PROMPT.getName(), Edit.BACK.getName());

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        message.setReplyMarkup(keyboardMarkup);
        return message;
    }

    /**
     * Creates delete filters menu message.
     *
     * @param chatId the chat ID
     * @return configured SendMessage
     */
    public SendMessage createDeleteMenuMessage(final String chatId) {
        final long chatIdLong = Long.parseLong(chatId);
        final String filtersSummary = buildFiltersSummary(chatIdLong);
        final SendMessage message = new SendMessage(chatId, filtersSummary);

        final List<KeyboardRow> keyboard = new ArrayList<>();
        addRow(keyboard, Delete.DATE.getName(), Delete.TIME.getName());
        addRow(keyboard, Delete.EXCLUDED.getName(), Delete.MANDATORY.getName());
        addRow(keyboard, Delete.AI_PROMPT.getName(), Delete.SUBS.getName());
        addRow(keyboard, Delete.BACK.getName());

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        message.setReplyMarkup(keyboardMarkup);
        return message;
    }

    /**
     * Builds a summary of current filters for the user.
     *
     * @param chatId the chat ID
     * @return formatted filters summary
     */
    private String buildFiltersSummary(final long chatId) {
        final StringBuilder builder = new StringBuilder(200);
        builder.append("⚙️ <b>Текущие фильтры:</b>\n");
        appendDateFilter(builder, chatId);
        appendTimeFilter(builder, chatId);
        appendExcludedGenresFilter(builder, chatId);
        appendMandatoryGenresFilter(builder, chatId);
        appendAiPromptFilter(builder, chatId);
        appendSubtitleFilter(builder, chatId);
        return builder.toString();
    }

    private void appendDateFilter(final StringBuilder builder, final long chatId) {
        final String dateValue = this.stateManager.hasDateFilter(chatId)
                ? this.stateManager.getDateFilter(chatId).toString()
                : "сегодня";
        builder.append("\n📅 <b>Дата:</b> ").append(dateValue);
    }

    private void appendTimeFilter(final StringBuilder builder, final long chatId) {
        final String timeValue = this.stateManager.getTimeFilter(chatId) != null
                ? this.stateManager.getTimeFilter(chatId)
                : "не задано";
        builder.append("\n⏰ <b>Время:</b> ").append(timeValue);
    }

    private void appendExcludedGenresFilter(final StringBuilder builder, final long chatId) {
        final String excludedValue = Genre.toStringOrDefault(
                this.stateManager.getExcludedGenres(chatId),
                Common.NOT_SET_UP.getName()
        );
        builder.append("\n🚫 <b>Исключения:</b> ").append(excludedValue);
    }

    private void appendMandatoryGenresFilter(final StringBuilder builder, final long chatId) {
        final String mandatoryValue = Genre.toStringOrDefault(
                this.stateManager.getMandatoryGenres(chatId),
                Common.NOT_SET_UP.getName()
        );
        builder.append("\n✅ <b>Предпочтения:</b> ").append(mandatoryValue);
    }

    private void appendAiPromptFilter(final StringBuilder builder, final long chatId) {
        final String aiValue = this.stateManager.getAiPrompt(chatId) != null
                ? this.stateManager.getAiPrompt(chatId)
                : "не задан";
        builder.append("\n🤖 <b>AI-запрос:</b> ").append(aiValue);
    }

    private void appendSubtitleFilter(final StringBuilder builder, final long chatId) {
        final String subsValue = this.stateManager.hasSubtitleFilter(chatId) ? "да" : "нет";
        builder.append("\n <b>Только фильмы с субтитрами: </b> ").append(subsValue);
    }

    /**
     * Helper method to add a keyboard row with two buttons.
     *
     * @param keyboard the keyboard list
     * @param button1 first button text
     * @param button2 second button text
     */
    private static void addRow(final List<KeyboardRow> keyboard, final String button1, final String button2) {
        final KeyboardRow row = new KeyboardRow();
        row.add(button1);
        row.add(button2);
        keyboard.add(row);
    }

    /**
     * Helper method to add a keyboard row with one button.
     *
     * @param keyboard the keyboard list
     * @param button the button text
     */
    private static void addRow(final List<KeyboardRow> keyboard, final String button) {
        final KeyboardRow row = new KeyboardRow();
        row.add(button);
        keyboard.add(row);
    }
}
