
package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.database.TicketLogDAO;
import com.discordticketbot.utils.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CloseRequestHandler {
    private final Map<String, GuildConfig> guildConfigs;
    private final CloseRequestDAO closeRequestDAO;
    private final TicketLogDAO ticketLogDAO;

    public CloseRequestHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.closeRequestDAO = new CloseRequestDAO();
        this.ticketLogDAO = new TicketLogDAO();
    }

    public void handleCloseRequest(SlashCommandInteractionEvent event) {
        GuildConfig config = guildConfigs.get(event.getGuild().getId());
        if (config == null || !config.isConfigured()) {
            event.reply("❌ This server's ticket system is not configured.").setEphemeral(true).queue();
            return;
        }

        if (!PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can request ticket closure.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        String userId = channel.getTopic();
        
        if (userId == null || userId.isEmpty()) {
            event.reply("❌ This doesn't appear to be a valid ticket channel.").setEphemeral(true).queue();
            return;
        }

        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "No reason provided";
        Integer timeoutHours = event.getOption("timeout") != null ? event.getOption("timeout").getAsInt() : null;

        // Check if there's already an active close request
        if (closeRequestDAO.hasActiveCloseRequest(channel.getId())) {
            event.reply("❌ There is already an active close request for this ticket.").setEphemeral(true).queue();
            return;
        }

        // Check if ticket is excluded from auto-close
        if (closeRequestDAO.isExcludedFromAutoClose(channel.getId()) && timeoutHours != null) {
            event.reply("⚠️ This ticket is excluded from auto-close. Timeout will not apply.").setEphemeral(true).queue();
            timeoutHours = null;
        }

        // Log close request to database
        closeRequestDAO.createCloseRequest(
                channel.getId(),
                event.getUser().getId(),
                userId,
                reason,
                timeoutHours
        );

        User ticketOwner = event.getJDA().getUserById(userId);
        String ownerMention = ticketOwner != null ? ticketOwner.getAsMention() : "<@" + userId + ">";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 Close Request")
                .setDescription("**" + event.getUser().getAsMention() + "** has requested to close this ticket.\n\n" +
                        "**Reason:** " + reason + "\n" +
                        (timeoutHours != null ? "**Auto-close timeout:** " + timeoutHours + " hours\n\n" : "\n") +
                        ownerMention + ", please confirm if your issue has been resolved:")
                .setColor(Color.ORANGE)
                .setFooter("Close request • " + event.getUser().getAsTag());

        if (timeoutHours != null) {
            embed.addField("⏰ Automatic Closure", 
                    "This ticket will be automatically closed in **" + timeoutHours + " hours** if no response is received.", 
                    false);
        }

        event.reply("✅ Close request sent to the ticket owner.")
                .setEphemeral(true).queue();

        channel.sendMessageEmbeds(embed.build())
                .addActionRow(
                        Button.success("confirm_close_request", "✅ Confirm Close"),
                        Button.danger("deny_close_request", "❌ Keep Open")
                ).queue(message -> {
                    // Update database with message ID for future reference
                    closeRequestDAO.updateCloseRequestMessageId(channel.getId(), message.getId());
                    
                    // Schedule timeout if specified
                    if (timeoutHours != null) {
                        scheduleAutoClose(channel, message.getId(), timeoutHours);
                    }
                });

        // Log to ticket logs
        ticketLogDAO.logCloseRequest(channel.getId(), event.getUser().getId(), reason, timeoutHours);
    }

    public void handleConfirmCloseRequest(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        String userId = channel.getTopic();
        
        if (userId == null || !userId.equals(event.getUser().getId())) {
            event.reply("❌ Only the ticket owner can respond to this close request.").setEphemeral(true).queue();
            return;
        }

        // Mark close request as confirmed
        closeRequestDAO.confirmCloseRequest(channel.getId(), event.getUser().getId());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Close Request Confirmed")
                .setDescription(event.getUser().getAsMention() + " has confirmed that their issue is resolved.\n\n" +
                        "**Response:** Confirmed at <t:" + (System.currentTimeMillis() / 1000L) + ":F>")
                .setColor(Color.GREEN)
                .setFooter("Close request confirmed");

        event.editMessageEmbeds(embed.build())
                .setComponents().queue();

        // Proceed with closing the ticket
        TicketHandler ticketHandler = new TicketHandler(guildConfigs);
        ticketHandler.showCloseOptions(event);
    }

    public void handleDenyCloseRequest(ButtonInteractionEvent event) {
        TextChannel channel = event.getChannel().asTextChannel();
        String userId = channel.getTopic();
        
        if (userId == null || !userId.equals(event.getUser().getId())) {
            event.reply("❌ Only the ticket owner can respond to this close request.").setEphemeral(true).queue();
            return;
        }

        // Mark close request as denied
        closeRequestDAO.denyCloseRequest(channel.getId(), event.getUser().getId());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("❌ Close Request Denied")
                .setDescription(event.getUser().getAsMention() + " has indicated that their issue is **not yet resolved**.\n\n" +
                        "**Response:** Denied at <t:" + (System.currentTimeMillis() / 1000L) + ":F>\n\n" +
                        "The ticket will remain open for further assistance.")
                .setColor(Color.RED)
                .setFooter("Close request denied");

        event.editMessageEmbeds(embed.build())
                .setComponents().queue();

        // Log denial
        ticketLogDAO.logCloseRequestDenied(channel.getId(), event.getUser().getId());
    }

    private void scheduleAutoClose(TextChannel channel, String messageId, int timeoutHours) {
        // Schedule the auto-close task
        channel.getJDA().getThreadScheduler().schedule(() -> {
            // Check if request is still active
            if (!closeRequestDAO.hasActiveCloseRequest(channel.getId())) {
                return; // Request was already handled
            }

            // Auto-close the ticket
            closeRequestDAO.autoCloseRequest(channel.getId());
            
            EmbedBuilder timeoutEmbed = new EmbedBuilder()
                    .setTitle("⏰ Auto-Close Timeout")
                    .setDescription("This ticket has been automatically closed due to no response within " + timeoutHours + " hours.\n\n" +
                            "**Auto-closed at:** <t:" + (System.currentTimeMillis() / 1000L) + ":F>")
                    .setColor(Color.GRAY)
                    .setFooter("Ticket auto-closed due to timeout");

            channel.sendMessageEmbeds(timeoutEmbed.build()).queue();

            // Log auto-closure
            ticketLogDAO.logTicketAutoClosed(channel.getId(), timeoutHours);

            // Close the ticket after a brief delay
            TicketHandler ticketHandler = new TicketHandler(guildConfigs);
            // Create a mock button event for closing
            channel.delete().queueAfter(30, TimeUnit.SECONDS);
            
        }, timeoutHours, TimeUnit.HOURS);
    }

    public void handleAutoCloseExclude(SlashCommandInteractionEvent event) {
        GuildConfig config = guildConfigs.get(event.getGuild().getId());
        if (config == null || !PermissionUtil.hasStaffPermission(event.getMember(), config)) {
            event.reply("❌ Only staff members can exclude tickets from auto-close.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        if (channel.getTopic() == null || channel.getTopic().isEmpty()) {
            event.reply("❌ This doesn't appear to be a valid ticket channel.").setEphemeral(true).queue();
            return;
        }

        closeRequestDAO.excludeFromAutoClose(channel.getId(), event.getUser().getId());

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔒 Auto-Close Exclusion")
                .setDescription("This ticket has been **excluded from automatic closure**.\n\n" +
                        "Close request timeouts will not apply to this ticket.\n" +
                        "**Excluded by:** " + event.getUser().getAsMention())
                .setColor(Color.BLUE)
                .setFooter("Auto-close exclusion active");

        event.reply("✅ This ticket has been excluded from auto-close timeouts.").setEphemeral(true).queue();
        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
