package org.example;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {

        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);

            BotService botService = new BotService();

            telegramBotsApi.registerBot(botService);

            System.out.println("Bot started");

        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }
}
