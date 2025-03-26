package org.example.Logger;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class TelegramHandler extends StreamHandler {
    @Override
    public void publish(LogRecord record) {
        MyFormatter formatter = new MyFormatter();
        String format = formatter.format(record);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId("5450913649");
        sendMessage.setText(format);

        TgSender tgSender = TgSender.getInstance();

        try {
            tgSender.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return record.getLevel() == Level.SEVERE;
    }
}
