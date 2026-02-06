package bots.services.handlers;

import bots.enums.Common;
import bots.enums.EditCommand;
import bots.enums.Guide;
import bots.enums.UserState;
import bots.services.KeyboardService;
import bots.services.UserService;
import filters.Genre;

import java.util.Set;
import java.util.stream.Collectors;

public class EditCommandHandler implements CommandHandler<EditCommand> {
    private final UserService userService;
    private final KeyboardService keyboardService;

    public EditCommandHandler(final UserService userService, final KeyboardService keyboardService) {
        this.userService = userService;
        this.keyboardService = keyboardService;
    }

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public void handle(final long chatId, final String chatIdStr, final EditCommand command) {
        switch (command) {
            case EXCLUDED:
                this.userService.setUserState(chatId, UserState.AWAITING_EXCLUDED);
                final String excludedAsStr = this.userService.getExcludedGenres(chatId)
                    .stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
                keyboardService.showMainKeyboard(
                    chatIdStr,
                    String.format(
                        "Текущие исключения: %s\n\n%s",
                        excludedAsStr.isEmpty() ? Common.NOT_SET_UP.getName() : excludedAsStr,
                        Guide.EXCLUDED.getName()
                    )
                );
                break;

            case MANDATORY:
                this.userService.setUserState(chatId, UserState.AWAITING_MANDATORY);
                final Set<Genre> currentMandatory = this.userService.getMandatoryGenres(chatId);
                if (currentMandatory != null && !currentMandatory.isEmpty()) {
                    final String mandatoryAsStr = currentMandatory
                        .stream()
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));
                    this.keyboardService.showMainKeyboard(
                        chatIdStr,
                        String.format(
                            "Текущие предпочтения: %s\n\n%s",
                            mandatoryAsStr.isEmpty() ? Common.NOT_SET_UP.getName() : mandatoryAsStr,
                            Guide.MANDATORY.getName()
                        )
                    );
                }
                break;

            case TIME:
                this.userService.setUserState(chatId, UserState.AWAITING_TIME);
                final String currentTime = this.userService.getTimeFilter(chatId) != null ? this.userService.getTimeFilter(chatId) : Common.NOT_SET_UP.getName();
                keyboardService.showMainKeyboard(
                    chatIdStr,
                    String.format("Текущее время: %s\n\n%s", currentTime, Guide.TIME.getName())
                );
                break;

            case AI_PROMPT:
                userService.setUserState(chatId, UserState.AWAITING_AI);
                final String currentPrompt = this.userService.getAiPrompt(chatId) != null ? this.userService.getAiPrompt(chatId) : Common.NOT_SET_UP.getName();
                keyboardService.showMainKeyboard(
                    chatIdStr,
                    String.format("Текущий AI-запрос: %s\n\n%s", currentPrompt, Guide.AI_PROMPT.getName())
                );
                break;

            case BACK:
                userService.setUserState(chatId, UserState.IDLE);
                keyboardService.showMainKeyboard(chatIdStr, "Возврат в главное меню");
                break;

            default:
                break;
        }
    }
}