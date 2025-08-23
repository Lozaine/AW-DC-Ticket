package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.handlers.TicketHandler;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.Map;

public class ButtonListener extends ListenerAdapter {
    private final Map<String, GuildConfig> guildConfigs;
    private final TicketHandler ticketHandler;

    public ButtonListener(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.ticketHandler = new TicketHandler(guildConfigs);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        if (buttonId == null) return;

        switch (buttonId) {
            case "create_report_ticket" -> ticketHandler.createTicket(event, "Report", "🚨", Color.RED);
            case "create_support_ticket" -> ticketHandler.createTicket(event, "Support", "💬", Color.BLUE);
            case "create_appeal_ticket" -> ticketHandler.createTicket(event, "Appeal", "⚖️", Color.ORANGE);
            case "close_ticket" -> ticketHandler.showCloseOptions(event);
            case "reopen_ticket" -> ticketHandler.reopenTicket(event);
            case "generate_transcript" -> ticketHandler.generateAndSendTranscript(event);
            case "delete_ticket" -> ticketHandler.deleteTicket(event);
        }
    }
}