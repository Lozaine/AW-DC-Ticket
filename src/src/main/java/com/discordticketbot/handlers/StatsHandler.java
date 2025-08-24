package com.discordticketbot.handlers;

import com.discordticketbot.database.TicketLogDAO;
import com.discordticketbot.database.TicketLogDAO.TicketStats;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Guild;

public class StatsHandler extends ListenerAdapter {
    private final TicketLogDAO ticketLogDAO;

    public StatsHandler() {
        this.ticketLogDAO = new TicketLogDAO();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("stats")) return;

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ This command must be used in a server.").setEphemeral(true).queue();
            return;
        }

        TicketStats stats = ticketLogDAO.getGuildTicketStats(guild.getId());
        String reply = String.format(
                "**Ticket Statistics for this server**\n" +
                        "• Total: %d\n" +
                        "• Open: %d\n" +
                        "• Closed: %d\n" +
                        "• Deleted: %d",
                stats.totalTickets,
                stats.openTickets,
                stats.closedTickets,
                stats.deletedTickets
        );

        event.reply(reply).queue();
    }
}
