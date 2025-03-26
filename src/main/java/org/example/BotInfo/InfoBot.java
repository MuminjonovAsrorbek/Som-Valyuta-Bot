package org.example.BotInfo;

import io.github.cdimascio.dotenv.Dotenv;


public interface InfoBot {
    Dotenv dotenv = Dotenv.configure()
            .directory("src/main/resources/")
            .filename(".env")
            .load();


    String BOT_TOKEN = dotenv.get("BOT_TOKEN");

    String CBU_API_URL = "https://cbu.uz/uz/arkhiv-kursov-valyut/json/";

    String BOT_USERNAME = dotenv.get("BOT_USERNAME");

    String ADMIN_CHAT_ID = dotenv.get("ADMIN_CHAT_ID");
}
