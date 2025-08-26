package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.database.CloseRequestDAO;
import com.discordticketbot.database.TicketLogDAO;
import com.discordticketbot.utils.PermissionUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Map;

public class CleanupHandler {
    private final Map<String, GuildConfig> guildConfigs;
    private final TicketLogDAO ticketLogDAO;
    private final CloseRequestDAO closeRequestDAO;

    public CleanupHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.ticketLogDAO = new TicketLogDAO();
        this.closeRequestDAO = new CloseRequestDAO();
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasAdminPermission(event.getMember())) {
            event.reply("❌ You need Administrator permissions to use this command.").setEphemeral(true).queue();
            return;
        }

        var guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }

        GuildConfig config = guildConfigs.get(guild.getId());
        if (config == null) {
            config = GuildConfig.load(guild.getId());
            if (config == null) {
                config = new GuildConfig();
                config.setGuildId(guild.getId());
            }
            guildConfigs.put(guild.getId(), config);
        }

        Integer logsDays = event.getOption("logs_days") != null ? event.getOption("logs_days").getAsInt() : null;
        Integer requestsDays = event.getOption("requests_days") != null ? event.getOption("requests_days").getAsInt() : null;

        if (logsDays != null) config.cleanupTicketLogsDays = logsDays;
        if (requestsDays != null) config.cleanupCloseRequestsDays = requestsDays;

        config.save();

        if (logsDays != null) {
            ticketLogDAO.cleanupOldTicketLogs(config.cleanupTicketLogsDays);
        }
        if (requestsDays != null) {
            closeRequestDAO.cleanupOldCloseRequests(config.cleanupCloseRequestsDays);
        }

        event.reply("✅ Cleanup policies updated. Logs: " + config.cleanupTicketLogsDays + " days, Requests: " + config.cleanupCloseRequestsDays + " days.").setEphemeral(true).queue();
    }
}


