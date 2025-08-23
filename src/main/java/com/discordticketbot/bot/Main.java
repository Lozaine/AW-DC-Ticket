package com.discordticketbot.bot;

import com.discordticketbot.bot.TicketBot;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        String botToken;

        // Try to load from environment variables first (for production)
        botToken = System.getenv("BOT_TOKEN");

        // If not found in environment, try .env file (for development)
        if (botToken == null || botToken.isBlank()) {
            try {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                botToken = dotenv.get("BOT_TOKEN");
            } catch (Exception e) {
                System.out.println("Note: .env file not found, using environment variables");
            }
        }

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