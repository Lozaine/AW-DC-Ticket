package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.handlers.TicketHandler;
import com.discordticketbot.handlers.CloseRequestHandler;
import com.discordticketbot.utils.ErrorLogger;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.Map;

public class ButtonListener extends ListenerAdapter {
    private final Map<String, GuildConfig> guildConfigs;
    private final TicketHandler ticketHandler;
    private final CloseRequestHandler closeRequestHandler;
    private final ErrorLogger errorLogger;

    public ButtonListener(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.ticketHandler = new TicketHandler(guildConfigs);
        this.closeRequestHandler = new CloseRequestHandler(guildConfigs);
        this.errorLogger = new ErrorLogger(guildConfigs);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        try {
            switch (buttonId) {
                // Ticket creation buttons
                case "create_support_ticket" ->
                        ticketHandler.createTicket(event, "Support", "🎫", Color.BLUE);
                case "create_report_ticket" ->
                        ticketHandler.createTicket(event, "Report", "⚠️", Color.ORANGE);
                case "create_appeal_ticket" ->
                        ticketHandler.createTicket(event, "Appeal", "⚖️", Color.MAGENTA);

                // Ticket management buttons
                case "close_ticket" ->
                        ticketHandler.requestCloseReason(event);
                case "close_ticket_with_reason" ->
                        ticketHandler.showCloseOptions(event);
                case "reopen_ticket" ->
                        ticketHandler.reopenTicket(event);
                case "generate_transcript" ->
                        ticketHandler.generateAndSendTranscript(event);
                case "generate_html_transcript" ->
                        ticketHandler.generateHtmlTranscript(event);
                case "delete_ticket" ->
                        ticketHandler.deleteTicket(event);

                // Close request buttons
                case "confirm_close_request" ->
                        closeRequestHandler.handleConfirmCloseRequest(event);
                case "deny_close_request" ->
                        closeRequestHandler.handleDenyCloseRequest(event);

                default -> event.reply("❌ Unknown button: " + buttonId).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            errorLogger.logError(event.getGuild(), "Button Interaction: " + buttonId,
                    "Error handling button interaction: " + e.getMessage(), e, event.getUser());

            if (!event.isAcknowledged()) {
                event.reply("❌ An error occurred while processing your request. Please try again or contact an administrator.")
                        .setEphemeral(true).queue();
            } else {
                event.getHook().sendMessage("❌ An error occurred while processing your request. Please try again or contact an administrator.")
                        .setEphemeral(true).queue();
            }
        }
    }
}