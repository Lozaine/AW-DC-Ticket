package com.discordticketbot.bot;

import com.discordticketbot.bot.TicketBot;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Discord Ticket Bot...");

        String botToken;

        // Try to load from environment variables first (for production)
        botToken = System.getenv("BOT_TOKEN");

        // If not found in environment, try .env file (for development)
        if (botToken == null || botToken.isBlank()) {
            try {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                botToken = dotenv.get("BOT_TOKEN");
                System.out.println("✅ Loaded configuration from .env file");
            } catch (Exception e) {
                System.out.println("📝 Note: .env file not found, using environment variables");
            }
        }

        if (botToken == null || botToken.isBlank()) {
            System.err.println("❌ BOT_TOKEN environment variable is not set.");
            System.err.println("💡 Please set BOT_TOKEN in your environment variables or create a .env file");
            System.exit(1);
        }

        try {
            System.out.println("🔌 Initializing database connection...");
            TicketBot bot = new TicketBot(botToken);

            System.out.println("🤖 Starting Discord bot...");
            bot.start();

            System.out.println("✅ Discord Ticket Bot started successfully!");
            System.out.println("🎫 Bot is ready to handle tickets!");

        } catch (Exception e) {
            System.err.println("❌ Failed to start bot: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}