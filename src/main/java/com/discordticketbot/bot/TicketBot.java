package com.discordticketbot.bot;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.database.DatabaseManager;
import com.discordticketbot.database.GuildConfigDAO;
import com.discordticketbot.listeners.CommandListener;
import com.discordticketbot.listeners.ButtonListener;
import com.discordticketbot.listeners.ReadyListener;
import com.discordticketbot.listeners.ModalListener; // Import ModalListener
import com.discordticketbot.utils.CommandDiagnosticUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import java.util.HashMap;
import java.util.Map;

public class TicketBot {
    private final String botToken;
    private JDA jda;
    private final Map<String, GuildConfig> guildConfigs = new HashMap<>();
    private GuildConfigDAO guildConfigDAO;

    public TicketBot(String botToken) {
        this.botToken = botToken;

        // Initialize database connection
        try {
            System.out.println("🔄 Initializing database connection...");
            DatabaseManager.getInstance(); // Initialize database
            this.guildConfigDAO = new GuildConfigDAO();
            System.out.println("✅ Database initialized successfully!");

            // Test database connection
            System.out.println("🔄 Testing database connection...");
            DatabaseManager.getInstance().getConnection().close();
            System.out.println("✅ Database connection test successful!");

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public void start() throws Exception {
        // Load existing guild configurations from database
        loadGuildConfigsFromDatabase();

        jda = JDABuilder.createDefault(botToken)
                .addEventListeners(
                        new ReadyListener(jda, guildConfigs),
                        new CommandListener(guildConfigs),
                        new ButtonListener(guildConfigs),
                        new ModalListener(guildConfigs)
                )
                .build();

        // Wait for bot to be ready, then print invite URLs
        jda.awaitReady();

        // Print optimized invite URL for command visibility
        CommandDiagnosticUtil.printOptimizedInviteUrl(jda);

        // Test command availability after a short delay
        Thread.sleep(5000); // Wait 5 seconds
        CommandDiagnosticUtil.printCommandDiagnostics(jda);

        // Add shutdown hook to properly close database connections
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**
     * Load all guild configurations from database into memory
     */
    private void loadGuildConfigsFromDatabase() {
        try {
            System.out.println("🔄 Loading guild configurations from database...");
            Map<String, GuildConfig> loadedConfigs = guildConfigDAO.loadAllGuildConfigs();

            // Set guild IDs and prepare configs for runtime use
            for (Map.Entry<String, GuildConfig> entry : loadedConfigs.entrySet()) {
                String guildId = entry.getKey();
                GuildConfig config = entry.getValue();
                config.setGuildId(guildId);
                guildConfigs.put(guildId, config);

                // Debug: Print loaded config details
                System.out.println("📋 Loaded config for guild " + guildId + ":");
                System.out.println("   - Category ID: " + config.categoryId);
                System.out.println("   - Panel Channel ID: " + config.panelChannelId);
                System.out.println("   - Transcript Channel ID: " + config.transcriptChannelId);
                System.out.println("   - Support Roles: " + config.supportRoleIds.size());
                System.out.println("   - Ticket Counter: " + config.ticketCounter);
            }

            System.out.println("✅ Loaded " + guildConfigs.size() + " guild configurations from database");

        } catch (Exception e) {
            System.err.println("❌ Failed to load guild configurations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save guild configuration to both memory and database
     */
    public void saveGuildConfig(String guildId, GuildConfig config) {
        config.setGuildId(guildId);
        guildConfigs.put(guildId, config);
        config.save(); // This will save to database
        System.out.println("✅ Guild configuration saved: " + guildId);
    }

    /**
     * Get guild configuration (loads from database if not in memory)
     */
    public GuildConfig getGuildConfig(String guildId) {
        GuildConfig config = guildConfigs.get(guildId);

        if (config == null) {
            // Try to load from database
            config = GuildConfig.load(guildId);
            if (config != null) {
                guildConfigs.put(guildId, config);
                System.out.println("✅ Loaded guild config from database: " + guildId);
            }
        }

        return config;
    }

    /**
     * Shutdown method to properly close database connections
     */
    public void shutdown() {
        try {
            if (jda != null) {
                jda.shutdown();
                System.out.println("JDA shutdown completed");
            }

            DatabaseManager.getInstance().close();
            System.out.println("Database connections closed");

        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public JDA getJda() {
        return jda;
    }

    public Map<String, GuildConfig> getGuildConfigs() {
        return guildConfigs;
    }
}