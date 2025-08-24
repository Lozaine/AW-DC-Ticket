package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.PermissionUtil;
import com.discordticketbot.utils.TicketPanelUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Map;

public class PanelHandler {
    private final Map<String, GuildConfig> guildConfigs;

    public PanelHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasAdminPermission(event.getMember())) {
            event.reply("❌ You need Administrator permissions to use this command.")
                    .setEphemeral(true).queue();
            return;
        }

        // Check bot Administrator permission
        if (!event.getGuild().getSelfMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("❌ I need Administrator permission to manage the ticket system. Please grant this permission and try again.")
                    .setEphemeral(true).queue();
            return;
        }

        GuildConfig config = guildConfigs.get(event.getGuild().getId());
        if (config == null || !config.isConfigured()) {
            event.reply("❌ Please configure the bot first using `/setup`!").setEphemeral(true).queue();
            return;
        }

        TextChannel panelChannel = event.getGuild().getTextChannelById(config.panelChannelId);
        if (panelChannel == null) {
            event.reply("❌ Configured panel channel not found. Please reconfigure with `/setup`.")
                    .setEphemeral(true).queue();
            return;
        }

        TicketPanelUtil.createTicketPanel(panelChannel);
        event.reply("✅ Ticket panel has been sent to: " + panelChannel.getAsMention())
                .setEphemeral(true).queue();
    }
}