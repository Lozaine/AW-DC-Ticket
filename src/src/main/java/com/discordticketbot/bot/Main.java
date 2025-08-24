package com.discordticketbot.bot;

import com.discordticketbot.bot.TicketBot;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Discord Ticket Bot...");

        // Display startup banner
        displayStartupBanner();

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
            System.out.println("📋 Slash commands will appear in bot profile with {/} buttons");

            // Display helpful information
            displayPostStartupInfo();

        } catch (Exception e) {
            System.err.println("❌ Failed to start bot: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void displayStartupBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     Discord Ticket Bot v1.1.0                  ║");
        System.out.println("║                                                                  ║");
        System.out.println("║  Features:                                                       ║");
        System.out.println("║  • Slash Commands with {/} profile integration                  ║");
        System.out.println("║  • Persistent ticket numbering                                  ║");
        System.out.println("║  • Database-backed configuration                                ║");
        System.out.println("║  • Automatic transcript generation                              ║");
        System.out.println("║  • Multi-type tickets (Report, Support, Appeal)                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void displayPostStartupInfo() {
        System.out.println("\n📌 Important Notes:");
        System.out.println("• Global slash commands take up to 1 hour to sync across Discord");
        System.out.println("• Commands will appear in bot's profile with {/} buttons");
        System.out.println("• Use /help in Discord to see all available commands");
        System.out.println("• Use /setup to configure the ticket system in your server");
        System.out.println("\n🔗 Bot Invite Link Requirements:");
        System.out.println("• Make sure bot has 'applications.commands' scope");
        System.out.println("• Administrator permission recommended for full functionality");
        System.out.println();
    }
}