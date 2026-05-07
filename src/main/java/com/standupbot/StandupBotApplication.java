package com.standupbot;

import com.standupbot.config.AppConfig;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;

public class StandupBotApplication {

    public static void main(String[] args) {
        Dotenv.configure().ignoreIfMissing().systemProperties().load();

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT",
                        System.getProperty("PORT", "7070")));

        Javalin app = AppConfig.buildApp();
        app.start(port);
    }
}
