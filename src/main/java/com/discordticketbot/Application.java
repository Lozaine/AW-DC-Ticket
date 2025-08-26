package com.discordticketbot;

import com.discordticketbot.bot.TicketBot;

public class Application {

    public static void main(String[] args) {
        // Start the Discord bot
        try {
            String botToken = System.getenv("BOT_TOKEN");
            if (botToken != null && !botToken.isBlank()) {
                System.out.println("🤖 Starting Discord bot...");
                TicketBot bot = new TicketBot(botToken);
                bot.start();
                
                System.out.println("✅ Discord bot started successfully!");
                
                // Keep the application running
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("🛑 Shutting down Discord bot...");
                    bot.shutdown();
                }));
                
                // Wait indefinitely
                Thread.currentThread().join();
            } else {
                System.err.println("❌ BOT_TOKEN environment variable is not set.");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to start Discord bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}