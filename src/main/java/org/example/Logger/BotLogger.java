package org.example.Logger;

import org.example.BotService;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BotLogger {

    private static final Logger logger = Logger.getLogger(BotService.class.getName());

    static {
        try {
            FileHandler fileHandler = new FileHandler("D:\\Project's\\JavaProject's\\ifoda-bot\\src\\main\\resources\\Exceptions\\error.log", true);

            TelegramHandler telegramHandler = new TelegramHandler();

            MyFormatter formatter = new MyFormatter();

            fileHandler.setFormatter(formatter);

            logger.addHandler(fileHandler);
            logger.addHandler(telegramHandler);
            logger.setLevel(Level.SEVERE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logError(String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
    }

}
