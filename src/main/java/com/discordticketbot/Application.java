package com.discordticketbot;

import com.discordticketbot.bot.TicketBot;
import com.discordticketbot.utils.HttpServerUtil;

public class Application {

    public static void main(String[] args) {
        // Start the HTTP server for transcripts
        try {
            System.out.println("🌐 Starting HTTP server for transcripts...");
            HttpServerUtil.startServer();
            System.out.println("✅ HTTP server started successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }

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
                    
                    System.out.println("🛑 Shutting down HTTP server...");
                    HttpServerUtil.stopServer();
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