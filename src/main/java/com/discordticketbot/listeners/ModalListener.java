package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.handlers.TicketHandler;
import com.discordticketbot.utils.ErrorLogger;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Map;

public class ModalListener extends ListenerAdapter {
    private final Map<String, GuildConfig> guildConfigs;
    private final TicketHandler ticketHandler;
    private final ErrorLogger errorLogger;

    public ModalListener(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.ticketHandler = new TicketHandler(guildConfigs);
        this.errorLogger = new ErrorLogger(guildConfigs);
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        try {
            switch (modalId) {
                case "close_reason_modal" -> ticketHandler.handleCloseReasonModal(event);
                default -> event.reply("❌ Unknown modal: " + modalId).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            errorLogger.logError(event.getGuild(), "Modal Interaction: " + modalId,
                    "Error handling modal interaction: " + e.getMessage(), e, event.getUser());

            if (!event.isAcknowledged()) {
                event.reply("❌ An error occurred while processing your submission. Please try again or contact an administrator.")
                        .setEphemeral(true).queue();
            } else {
                event.getHook().sendMessage("❌ An error occurred while processing your submission. Please try again or contact an administrator.")
                        .setEphemeral(true).queue();
            }
        }
    }
}