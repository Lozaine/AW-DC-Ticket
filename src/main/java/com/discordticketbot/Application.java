package com.discordticketbot;

import com.discordticketbot.bot.TicketBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        
        // Start the Discord bot
        try {
            String botToken = System.getenv("BOT_TOKEN");
            if (botToken != null && !botToken.isBlank()) {
                System.out.println("🤖 Starting Discord bot from Spring Boot application...");
                TicketBot bot = new TicketBot(botToken);
                bot.start();
                
                // Register the bot instance in the Spring context
                context.getBeanFactory().registerSingleton("ticketBot", bot);
                
                System.out.println("✅ Discord bot started successfully from Spring Boot application!");
            } else {
                System.err.println("❌ BOT_TOKEN environment variable is not set.");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to start Discord bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}