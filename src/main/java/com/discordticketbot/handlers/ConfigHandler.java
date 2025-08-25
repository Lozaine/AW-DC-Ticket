package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.database.DatabaseManager;
import com.discordticketbot.utils.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;


import java.awt.*;
import java.util.Map;

public class ConfigHandler {
    private final Map<String, GuildConfig> guildConfigs;

    public ConfigHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
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

        EmbedBuilder configEmbed = new EmbedBuilder()
                .setTitle("🔧 Current Bot Configuration")
                .setColor(config != null && config.isConfigured() ? Color.GREEN : Color.RED)
                .setThumbnail(guild.getIconUrl())
                .setFooter("Configuration for " + guild.getName(), guild.getIconUrl());

        if (config == null || !config.isConfigured()) {
            configEmbed.setDescription("❌ **Ticket system is not configured**\n\n" +
                            "Use `/setup` to configure the ticket system with:\n" +
                            "• Category for ticket channels\n" +
                            "• Panel channel for ticket creation\n" +
                            "• Transcript channel for logs\n" +
                            "• Support roles for staff access\n" +
                            "• Optional error log channel")
                    .addField("📋 Next Steps",
                            "1. Run `/setup` command\n" +
                                    "2. Configure all required settings\n" +
                                    "3. Run `/panel` to send ticket panel\n" +
                                    "4. Start using the ticket system!", false);
        } else {
            // Database connection status
            boolean dbConnected = testDatabaseConnection();
            configEmbed.addField("💾 Database Status",
                    dbConnected ? "✅ Connected to PostgreSQL" : "❌ Database connection failed", true);

            // Category configuration
            Category category = guild.getCategoryById(config.categoryId);
            configEmbed.addField("📁 Ticket Category",
                    category != null ? category.getAsMention() + "\n`" + category.getId() + "`" :
                            "❌ Category not found: `" + config.categoryId + "`", true);

            // Panel channel configuration
            TextChannel panelChannel = guild.getTextChannelById(config.panelChannelId);
            configEmbed.addField("📋 Panel Channel",
                    panelChannel != null ? panelChannel.getAsMention() + "\n`" + panelChannel.getId() + "`" :
                            "❌ Panel channel not found: `" + config.panelChannelId + "`", true);

            // Transcript channel configuration
            TextChannel transcriptChannel = guild.getTextChannelById(config.transcriptChannelId);
            configEmbed.addField("📜 Transcript Channel",
                    transcriptChannel != null ? transcriptChannel.getAsMention() + "\n`" + transcriptChannel.getId() + "`" :
                            "❌ Transcript channel not found: `" + config.transcriptChannelId + "`", true);

            // Error log channel configuration
            if (config.errorLogChannelId != null) {
                TextChannel errorChannel = guild.getTextChannelById(config.errorLogChannelId);
                configEmbed.addField("🚨 Error Log Channel",
                        errorChannel != null ? errorChannel.getAsMention() + "\n`" + errorChannel.getId() + "`" :
                                "❌ Error channel not found: `" + config.errorLogChannelId + "`", true);
            } else {
                configEmbed.addField("🚨 Error Log Channel", "Not configured\n(Errors log to console)", true);
            }

            // Ticket counter
            configEmbed.addField("🔢 Ticket Counter",
                    "Current: **" + config.ticketCounter + "**\n" +
                            "Next ticket: **#" + String.format("%03d", config.ticketCounter + 1) + "**", true);

            // Support roles configuration
            StringBuilder rolesText = new StringBuilder();
            if (config.supportRoleIds.isEmpty()) {
                rolesText.append("❌ No support roles configured");
            } else {
                for (String roleId : config.supportRoleIds) {
                    Role role = guild.getRoleById(roleId);
                    if (role != null) {
                        rolesText.append("✅ ").append(role.getAsMention()).append("\n");
                    } else {
                        rolesText.append("❌ Role not found: `").append(roleId).append("`\n");
                    }
                }
            }
            configEmbed.addField("👥 Support Roles (" + config.supportRoleIds.size() + ")",
                    rolesText.toString().trim(), false);

            // System status
            StringBuilder statusText = new StringBuilder();
            statusText.append("**Configuration Status:** ").append(config.isConfigured() ? "✅ Complete" : "❌ Incomplete").append("\n");
            statusText.append("**Database Storage:** ").append(dbConnected ? "✅ Active" : "❌ Failed").append("\n");
            statusText.append("**Bot Permissions:** ");

            if (guild.getSelfMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
                statusText.append("✅ Administrator\n");
            } else {
                statusText.append("⚠️ Limited (Administrator recommended)\n");
            }

            configEmbed.addField("📊 System Status", statusText.toString(), false);

            // Configuration actions
            configEmbed.addField("⚙️ Available Actions",
                    "• `/setup` - Reconfigure system settings\n" +
                            "• `/panel` - Send/update ticket creation panel\n" +
                            "• `/stats` - View ticket usage statistics\n" +
                            "• `/help` - View all available commands", false);
        }

        event.replyEmbeds(configEmbed.build()).setEphemeral(true).queue();
    }

    private boolean testDatabaseConnection() {
        try {
            return DatabaseManager.getInstance().testConnection();
        } catch (Exception e) {
            return false;
        }
    }
}