
package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.handlers.TicketHandler;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Map;

public class ModalListener extends ListenerAdapter {
    private final Map<String, GuildConfig> guildConfigs;
    private final TicketHandler ticketHandler;

    public ModalListener(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.ticketHandler = new TicketHandler(guildConfigs);
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        switch (event.getModalId()) {
            case "close_reason_modal" -> ticketHandler.handleCloseReasonModal(event);
        }
    }
}
