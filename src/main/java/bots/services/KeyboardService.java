package bots.services;

import bots.enums.Common;
import bots.enums.DeleteCommand;
import bots.enums.EditCommand;
import bots.enums.TriggerCommand;
import filters.Genre;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PMD.LooseCoupling", "PMD.ConsecutiveLiteralAppends"})
public class KeyboardService {
    private final MessageSender messageSender;
    private final UserService userService;

    public KeyboardService(final MessageSender messageSender, final UserService userService) {
        this.messageSender = messageSender;
        this.userService = userService;
    }

    public void showMainKeyboard(final String chatId, final String message) {
        final SendMessage sendMessage = new SendMessage(chatId, message);
        final List<KeyboardRow> keyboard = new ArrayList<>();

        final KeyboardRow row1 = new KeyboardRow();
        row1.add(TriggerCommand.TIME.getName());
        row1.add(TriggerCommand.DATE.getName());

        final KeyboardRow row2 = new KeyboardRow();
        row2.add(TriggerCommand.EXCLUDED.getName());
        row2.add(TriggerCommand.MANDATORY.getName());

        final KeyboardRow row3 = new KeyboardRow();
        row3.add(TriggerCommand.AI_PROMPT.getName());
        row3.add(TriggerCommand.EDIT.getName());

        final KeyboardRow row4 = new KeyboardRow();
        row4.add(
            this.userService.hasSubsFilter(Long.parseLong(chatId))
                ? TriggerCommand.SUBS_DIS.getName()
                : TriggerCommand.SUBS_EN.getName()
        );
        row4.add(TriggerCommand.DELETE.getName());

        final KeyboardRow row5 = new KeyboardRow();
        row5.add(TriggerCommand.SEARCH.getName());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);
        keyboard.add(row5);

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        sendMessage.setReplyMarkup(keyboardMarkup);
        this.messageSender.sendMessage(sendMessage);
    }

    public void showEditMenu(final String chatIdStr) {
        final long chatId = Long.parseLong(chatIdStr);
        final StringBuilder builder = new StringBuilder(200);
        builder.append("⚙️ <b>Текущие фильтры:</b>\n")
            .append("\n📅 <b>Дата:</b> ")
            .append(this.userService.getDateFilter(chatId) != null ? this.userService.getDateFilter(chatId) : "сегодня")
            .append("\n⏰ <b>Время:</b> ").append(this.userService.getTimeFilter(chatId) != null ? this.userService.getTimeFilter(chatId) : "не задано")
            .append("\n🚫 <b>Исключения:</b> ")
            .append(Genre.toStringOrDefault(this.userService.getExcludedGenres(chatId), Common.NOT_SET_UP.getName()))
            .append("\n✅ <b>Предпочтения:</b> ")
            .append(Genre.toStringOrDefault(this.userService.getMandatoryGenres(chatId), Common.NOT_SET_UP.getName()))
            .append("\n🤖 <b>AI-запрос:</b> ").append(this.userService.getAiPrompt(chatId) != null ? this.userService.getAiPrompt(chatId) : "не задан")
            .append("\n <b>Только фильмы с субтитрами: </b> ").append(this.userService.hasSubsFilter(chatId) ? "да" : "нет");

        final SendMessage message = new SendMessage(chatIdStr, builder.toString());
        message.setParseMode(MessageSender.HTML);

        final List<KeyboardRow> keyboard = new ArrayList<>();

        final KeyboardRow row1 = new KeyboardRow();
        row1.add(EditCommand.DATE.getName());
        row1.add(EditCommand.TIME.getName());

        final KeyboardRow row2 = new KeyboardRow();
        row2.add(EditCommand.EXCLUDED.getName());
        row2.add(EditCommand.MANDATORY.getName());

        final KeyboardRow row3 = new KeyboardRow();
        row3.add(EditCommand.AI_PROMPT.getName());
        row3.add(EditCommand.BACK.getName());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        message.setReplyMarkup(keyboardMarkup);
        this.messageSender.sendMessage(message);
    }

    public void showDeleteMenu(final String chatIdStr) {
        final long chatId = Long.parseLong(chatIdStr);
        final StringBuilder builder = new StringBuilder(200);
        builder.append("⚙️ <b>Текущие фильтры:</b>\n")
            .append("\n📅 <b>Дата:</b> ")
            .append(this.userService.getDateFilter(chatId) != null ? this.userService.getDateFilter(chatId) : "сегодня")
            .append("\n⏰ <b>Время:</b> ").append(this.userService.getTimeFilter(chatId) != null ? this.userService.getTimeFilter(chatId) : "не задано")
            .append("\n🚫 <b>Исключения:</b> ")
            .append(Genre.toStringOrDefault(this.userService.getExcludedGenres(chatId), Common.NOT_SET_UP.getName()))
            .append("\n✅ <b>Предпочтения:</b> ")
            .append(Genre.toStringOrDefault(this.userService.getMandatoryGenres(chatId), Common.NOT_SET_UP.getName()))
            .append("\n🤖 <b>AI-запрос:</b> ").append(this.userService.getAiPrompt(chatId) != null ? this.userService.getAiPrompt(chatId) : "не задан")
            .append("\n <b>Только фильмы с субтитрами: </b> ")
            .append(this.userService.hasSubsFilter(chatId) ? "да" : "нет");

        final SendMessage message = new SendMessage(chatIdStr, builder.toString());
        message.setParseMode(MessageSender.HTML);

        final List<KeyboardRow> keyboard = new ArrayList<>();

        final KeyboardRow row1 = new KeyboardRow();
        row1.add(DeleteCommand.DATE.getName());
        row1.add(DeleteCommand.TIME.getName());

        final KeyboardRow row2 = new KeyboardRow();
        row2.add(DeleteCommand.EXCLUDED.getName());
        row2.add(DeleteCommand.MANDATORY.getName());

        final KeyboardRow row3 = new KeyboardRow();
        row3.add(DeleteCommand.AI_PROMPT.getName());
        row3.add(DeleteCommand.SUBS.getName());

        final KeyboardRow row4 = new KeyboardRow();
        row4.add(DeleteCommand.BACK.getName());

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        final ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        message.setReplyMarkup(keyboardMarkup);
        this.messageSender.sendMessage(message);
    }
}