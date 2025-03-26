package org.example.Logger;

import org.example.BotInfo.InfoBot;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;

public class TgSender extends DefaultAbsSender {

    protected TgSender(DefaultBotOptions options, String botToken) {
        super(options, botToken);
    }

    public static TgSender getInstance() {
        return new TgSender(new DefaultBotOptions(), InfoBot.BOT_TOKEN);
    }
}
