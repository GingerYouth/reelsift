package bots.services.handlers;

import bots.enums.Command;

public interface CommandHandler<T extends Command> {

    void handle(
        final long chatId,
        final String chatIdString,
        final T delete
    );

}
