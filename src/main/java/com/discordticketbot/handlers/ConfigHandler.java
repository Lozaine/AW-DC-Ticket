package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
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

        GuildConfig config = guildConfigs.get(event.getGuild().getId());
        if (config == null || !config.isConfigured()) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("⚠️ Bot Not Configured")
                    .setDescription("The ticket system has not been configured yet.\n\nUse `/setup` to configure the bot!")
                    .setColor(Color.YELLOW)
                    .setFooter("Configuration required");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        StringBuilder supportRolesStr = new StringBuilder();
        for (String roleId : config.supportRoleIds) {
            Role role = guild.getRoleById(roleId);
            if (role != null) supportRolesStr.append(role.getAsMention()).append(" ");
            else supportRolesStr.append("❌ Deleted Role (").append(roleId).append(") ");
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔧 Current Bot Configuration")
                .setDescription("Here's your current ticket system configuration:")
                .addField("📁 Ticket Category",
                        guild.getCategoryById(config.categoryId) != null ?
                                guild.getCategoryById(config.categoryId).getAsMention() : "❌ Not Found", true)
                .addField("📋 Panel Channel",
                        guild.getTextChannelById(config.panelChannelId) != null ?
                                guild.getTextChannelById(config.panelChannelId).getAsMention() : "❌ Not Found", true)
                .addField("👥 Support Roles", supportRolesStr.toString().trim(), false)
                .addField("📜 Transcript Channel",
                        guild.getTextChannelById(config.transcriptChannelId) != null ?
                                guild.getTextChannelById(config.transcriptChannelId).getAsMention() : "❌ Not Found", true)
                .addField("📊 Available Commands",
                        "• `/setup` - Configure ticket system\n" +
                                "• `/panel` - Send ticket creation panel\n" +
                                "• `/closerequest` - Request user confirmation to close\n" +
                                "• `/autoclose exclude` - Exclude from auto-close\n" +
                                "• `/stats` - View ticket statistics\n" +
                                "• `/config` - View current settings\n" +
                                "• `/help` - Show all commands\n" +
                                "• `Close Ticket` button - Close with reason modal", false)
                .setColor(Color.BLUE)
                .setFooter("Use /setup to reconfigure if needed");
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}