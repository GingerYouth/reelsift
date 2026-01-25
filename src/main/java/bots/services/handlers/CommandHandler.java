package bots.services.handlers;

import bots.enums.Command;

@FunctionalInterface
public interface CommandHandler<T extends Command> {

    void handle(
        long chatId,
        String chatIdString,
        T delete
    );

}
