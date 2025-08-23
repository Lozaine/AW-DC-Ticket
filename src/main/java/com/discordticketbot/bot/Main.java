package com.discordticketbot.bot;

import com.discordticketbot.bot.TicketBot;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String botToken = dotenv.get("BOT_TOKEN");

        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("BOT_TOKEN environment variable is not set.");
        }

        try {
            TicketBot bot = new TicketBot(botToken);
            bot.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}