package com.discordticketbot.handlers;

import com.discordticketbot.database.TicketLogDAO;
import com.discordticketbot.database.TicketLogDAO.TicketStats;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;

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

        // Display recent tickets
        StringBuilder recentTickets = new StringBuilder();
        List<TicketLogDAO.TicketLogEntry> recent = ticketLogDAO.getRecentTickets(guild.getId(), 5);

        if (recent.isEmpty()) {
            recentTickets.append("No recent tickets found");
        } else {
            for (TicketLogDAO.TicketLogEntry ticket : recent) {
                String userName = "Unknown User";
                try {
                    net.dv8tion.jda.api.entities.User user = guild.getJDA().getUserById(ticket.ownerId);
                    if (user != null) userName = user.getName();
                } catch (Exception e) {
                    // User not found or error retrieving
                }

                String statusInfo = ticket.status.toUpperCase();
                if (ticket.closeReason != null && !ticket.closeReason.equals("No reason provided")) {
                    statusInfo += " - " + (ticket.closeReason.length() > 30 ?
                            ticket.closeReason.substring(0, 30) + "..." : ticket.closeReason);
                }

                recentTickets.append(String.format("`#%03d` **%s** by %s\n*%s*\n",
                        ticket.ticketNumber,
                        ticket.ticketType,
                        userName,
                        statusInfo
                ));
            }
        }

        event.reply(reply + "\n\n" + recentTickets.toString()).queue();
    }
}