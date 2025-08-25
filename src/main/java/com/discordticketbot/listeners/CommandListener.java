package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.handlers.*;
import com.discordticketbot.utils.ErrorLogger;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Map;

public class CommandListener extends ListenerAdapter {
    private final Map<String, GuildConfig> guildConfigs;
    private final SetupHandler setupHandler;
    private final PanelHandler panelHandler;
    private final HelpHandler helpHandler;
    private final ConfigHandler configHandler;
    private final StatsHandler statsHandler;
    private final CloseRequestHandler closeRequestHandler;
    private final ErrorLogger errorLogger;

    public CommandListener(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.setupHandler = new SetupHandler(guildConfigs);
        this.panelHandler = new PanelHandler(guildConfigs);
        this.helpHandler = new HelpHandler(guildConfigs);
        this.configHandler = new ConfigHandler(guildConfigs);
        this.statsHandler = new StatsHandler(guildConfigs);
        this.closeRequestHandler = new CloseRequestHandler(guildConfigs);
        this.errorLogger = new ErrorLogger(guildConfigs);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();

        try {
            switch (commandName) {
                case "help" -> helpHandler.handle(event);
                case "setup" -> setupHandler.handle(event);
                case "panel" -> panelHandler.handle(event);
                case "config" -> configHandler.handle(event);
                case "stats" -> statsHandler.handle(event);
                case "closerequest" -> closeRequestHandler.handleCloseRequest(event);
                case "autoclose" -> {
                    if ("exclude".equals(event.getSubcommandName())) {
                        closeRequestHandler.handleAutoCloseExclude(event);
                    }
                }
                default -> event.reply("❌ Unknown command: " + commandName).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            errorLogger.logError(event.getGuild(), "Slash Command: " + commandName,
                    "Error executing command: " + e.getMessage(), e, event.getUser());

            if (!event.isAcknowledged()) {
                event.reply("❌ An error occurred while processing your command. Please try again or contact an administrator.")
                        .setEphemeral(true).queue();
            } else {
                event.getHook().sendMessage("❌ An error occurred while processing your command. Please try again or contact an administrator.")
                        .setEphemeral(true).queue();
            }
        }
    }
}