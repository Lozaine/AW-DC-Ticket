package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.PermissionUtil;
import com.discordticketbot.utils.TicketPanelUtil;
import com.discordticketbot.utils.ErrorLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.Map;

public class PanelHandler {
    private final Map<String, GuildConfig> guildConfigs;
    private final ErrorLogger errorLogger;

    public PanelHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.errorLogger = new ErrorLogger(guildConfigs);
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasAdminPermission(event.getMember())) {
            event.reply("❌ You need Administrator permissions to use this command.")
                    .setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ This command can only be used in a server.")
                    .setEphemeral(true).queue();
            return;
        }

        GuildConfig config = guildConfigs.get(guild.getId());
        if (config == null || !config.isConfigured()) {
            event.reply("❌ Ticket system is not configured. Please use `/setup` first.")
                    .setEphemeral(true).queue();
            return;
        }

        TextChannel panelChannel = guild.getTextChannelById(config.panelChannelId);
        if (panelChannel == null) {
            event.reply("❌ Panel channel not found. Please reconfigure using `/setup`.")
                    .setEphemeral(true).queue();
            return;
        }

        try {
            // Create the ticket panel embed
            EmbedBuilder panelEmbed = new EmbedBuilder()
                    .setTitle("🎫 Support Ticket System")
                    .setDescription("Welcome to our support system! Choose the appropriate ticket type below:\n\n" +
                            "🎫 **Support Ticket** - General help and questions\n" +
                            "⚠️ **Report Ticket** - Report users, bugs, or issues\n" +
                            "⚖️ **Appeal Ticket** - Appeal bans, mutes, or other punishments\n\n" +
                            "**Guidelines:**\n" +
                            "• Only create one ticket at a time\n" +
                            "• Provide detailed information about your issue\n" +
                            "• Be patient while waiting for staff response\n" +
                            "• Use the close button when your issue is resolved")
                    .setColor(Color.BLUE)
                    .setThumbnail(guild.getIconUrl())
                    .setFooter("Click a button below to create a ticket", guild.getIconUrl());

            // Send the panel to the configured channel
            panelChannel.sendMessageEmbeds(panelEmbed.build())
                    .setActionRow(
                            Button.primary("create_support_ticket", "🎫 Support Ticket"),
                            Button.secondary("create_report_ticket", "⚠️ Report Ticket"),
                            Button.success("create_appeal_ticket", "⚖️ Appeal Ticket")
                    ).queue(
                            success -> {
                                event.reply("✅ Ticket panel sent successfully to " + panelChannel.getAsMention() + "!")
                                        .setEphemeral(true).queue();

                                errorLogger.logSuccess(guild, "Panel Sent",
                                        "Ticket panel successfully sent to " + panelChannel.getName(), event.getUser());
                            },
                            failure -> {
                                errorLogger.logError(guild, "Send Panel",
                                        "Failed to send ticket panel to " + panelChannel.getName() + ": " + failure.getMessage(),
                                        new Exception(failure), event.getUser());

                                event.reply("❌ Failed to send ticket panel. Please check bot permissions.")
                                        .setEphemeral(true).queue();
                            }
                    );

        } catch (Exception e) {
            errorLogger.logError(guild, "Panel Handler",
                    "Error creating ticket panel: " + e.getMessage(), e, event.getUser());

            event.reply("❌ An error occurred while creating the ticket panel.")
                    .setEphemeral(true).queue();
        }
    }
}