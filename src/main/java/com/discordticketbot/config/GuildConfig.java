package com.discordticketbot.config;

import java.util.HashSet;
import java.util.Set;

public class GuildConfig {
    public String categoryId;
    public Set<String> supportRoleIds = new HashSet<>();
    public String transcriptChannelId;
    public String panelChannelId;
    public int ticketCounter = 0; // Global ticket counter that persists

    public boolean isConfigured() {
        return categoryId != null && !supportRoleIds.isEmpty()
                && transcriptChannelId != null && panelChannelId != null;
    }

    /**
     * Get the next ticket number and increment the counter
     */
    public synchronized int getNextTicketNumber() {
        return ++ticketCounter;
    }

    /**
     * Initialize counter based on existing tickets (called during bot startup)
     */
    public void initializeCounterFromExistingTickets(int highestExistingNumber) {
        this.ticketCounter = Math.max(this.ticketCounter, highestExistingNumber);
    }
}