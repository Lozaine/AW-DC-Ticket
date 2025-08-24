package com.discordticketbot.config;

import com.discordticketbot.database.GuildConfigDAO;

import java.util.HashSet;
import java.util.Set;

public class GuildConfig {
    public String categoryId;
    public Set<String> supportRoleIds = new HashSet<>();
    public String transcriptChannelId;
    public String panelChannelId;
    public int ticketCounter = 0; // Global ticket counter that persists

    // Reference to DAO for database operations
    private transient GuildConfigDAO dao;
    private transient String guildId;

    public GuildConfig() {
        this.dao = new GuildConfigDAO();
    }

    /**
     * Set the guild ID for this configuration (used for database operations)
     */
    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public boolean isConfigured() {
        return categoryId != null && !supportRoleIds.isEmpty()
                && transcriptChannelId != null && panelChannelId != null;
    }

    /**
     * Get the next ticket number and increment the counter
     * This method now persists the counter to the database
     */
    public synchronized int getNextTicketNumber() {
        this.ticketCounter++;

        // Save to database if guildId is set
        if (guildId != null && dao != null) {
            dao.updateTicketCounter(guildId, this.ticketCounter);
        }

        return this.ticketCounter;
    }

    /**
     * Initialize counter based on existing tickets (called during bot startup)
     */
    public void initializeCounterFromExistingTickets(int highestExistingNumber) {
        this.ticketCounter = Math.max(this.ticketCounter, highestExistingNumber);

        // Save to database if guildId is set
        if (guildId != null && dao != null) {
            dao.updateTicketCounter(guildId, this.ticketCounter);
        }
    }

    /**
     * Save this configuration to the database
     */
    public void save() {
        if (guildId != null && dao != null) {
            dao.saveGuildConfig(guildId, this);
        }
    }

    /**
     * Load configuration from database
     */
    public static GuildConfig load(String guildId) {
        GuildConfigDAO dao = new GuildConfigDAO();
        GuildConfig config = dao.loadGuildConfig(guildId);

        if (config != null) {
            config.setGuildId(guildId);
            config.dao = dao;
        }

        return config;
    }
}