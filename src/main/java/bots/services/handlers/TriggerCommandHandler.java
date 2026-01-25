package bots.services.handlers;

import bots.enums.Guide;
import bots.enums.TriggerCommand;
import bots.enums.UserState;
import bots.services.KeyboardService;
import bots.services.SearchService;
import bots.services.UserService;

import java.io.IOException;

public class TriggerCommandHandler implements CommandHandler<TriggerCommand> {
    private final UserService userService;
    private final KeyboardService keyboardService;
    private final SearchService searchService;

    public TriggerCommandHandler(
        final UserService userService,
        final KeyboardService keyboardService,
        final SearchService searchService
    ) {
        this.userService = userService;
        this.keyboardService = keyboardService;
        this.searchService = searchService;
    }

    @Override
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.AvoidThrowingRawExceptionTypes"})
    public void handle(final long chatId, final String chatIdStr, final TriggerCommand command) {
        switch (command) {
            case DATE:
                this.userService.setUserState(chatId, UserState.AWAITING_DATE);
                this.keyboardService.showMainKeyboard(chatIdStr, Guide.DATE.getName());
                break;

            case EXCLUDED:
                this.userService.setUserState(chatId, UserState.AWAITING_EXCLUDED);
                this.keyboardService.showMainKeyboard(chatIdStr, Guide.EXCLUDED.getName());
                break;

            case MANDATORY:
                this.userService.setUserState(chatId, UserState.AWAITING_MANDATORY);
                this.keyboardService.showMainKeyboard(chatIdStr, Guide.MANDATORY.getName());
                break;

            case TIME:
                this.userService.setUserState(chatId, UserState.AWAITING_TIME);
                this.keyboardService.showMainKeyboard(chatIdStr, Guide.TIME.getName());
                break;

            case AI_PROMPT:
                this.userService.setUserState(chatId, UserState.AWAITING_AI);
                this.keyboardService.showMainKeyboard(chatIdStr, Guide.AI_PROMPT.getName());
                break;

            case SUBS_EN:
                this.userService.addSubsFilter(chatId);
                this.userService.setUserState(chatId, UserState.IDLE);
                break;

            case SUBS_DIS:
                this.userService.removeSubsFilter(chatId);
                break;

            case EDIT:
                this.userService.setUserState(chatId, UserState.EDITING_FILTERS);
                this.keyboardService.showEditMenu(chatIdStr);
                break;

            case DELETE:
                this.userService.setUserState(chatId, UserState.DELETING_FILTERS);
                this.keyboardService.showDeleteMenu(chatIdStr);
                break;

            case SEARCH:
                this.userService.setUserState(chatId, UserState.SEARCHING);
                try {
                    this.searchService.performSearch(chatIdStr, chatId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;

            default:
                keyboardService.showMainKeyboard(chatIdStr, "Выберите действие: ");
        }
    }
}