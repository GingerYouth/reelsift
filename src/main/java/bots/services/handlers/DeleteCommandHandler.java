package bots.services.handlers;

import bots.enums.DeleteCommand;
import bots.enums.UserState;
import bots.services.KeyboardService;
import bots.services.UserService;

public class DeleteCommandHandler implements CommandHandler<DeleteCommand> {
    private final UserService userService;
    private final KeyboardService keyboardService;

    public DeleteCommandHandler(UserService userService, KeyboardService keyboardService) {
        this.userService = userService;
        this.keyboardService = keyboardService;
    }

    @Override
    public void handle(final long chatId, final String chatIdStr, final DeleteCommand command) {
        switch (command) {
            case EXCLUDED -> this.userService.removeExcludedGenres(chatId);
            case MANDATORY -> this.userService.removeMandatoryGenres(chatId);
            case DATE -> this.userService.removeDateFilter(chatId);
            case TIME -> this.userService.removeTimeFilter(chatId);
            case AI_PROMPT -> this.userService.removeAiPrompt(chatId);
            case SUBS -> this.userService.removeSubsFilter(chatId);
            case ALL -> this.userService.clearAllFilters(chatId);
            case BACK -> {
                this.userService.setUserState(chatId, UserState.IDLE);
                this.keyboardService.showMainKeyboard(chatIdStr, "Возврат в главное меню");
            }
        }
    }
}