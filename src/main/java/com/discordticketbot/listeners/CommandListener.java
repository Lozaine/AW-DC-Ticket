package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.handlers.ConfigHandler;
import com.discordticketbot.handlers.HelpHandler;
import com.discordticketbot.handlers.PanelHandler;
import com.discordticketbot.handlers.SetupHandler;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import com.discordticketbot.handlers.StatsHandler;

import java.util.Map;

public class CommandListener extends ListenerAdapter {
    private final Map<String, GuildConfig> guildConfigs;
    private final HelpHandler helpHandler;
    private final SetupHandler setupHandler;
    private final PanelHandler panelHandler;
    private final ConfigHandler configHandler;
    private final StatsHandler statsHandler;

    public CommandListener(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.helpHandler = new HelpHandler();
        this.setupHandler = new SetupHandler(guildConfigs);
        this.panelHandler = new PanelHandler(guildConfigs);
        this.configHandler = new ConfigHandler(guildConfigs);
        this.statsHandler = new StatsHandler();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        switch (commandName) {
            case "help" -> helpHandler.handle(event);
            case "setup" -> setupHandler.handle(event);
            case "panel" -> panelHandler.handle(event);
            case "config" -> configHandler.handle(event);
            case "stats" -> statsHandler.onSlashCommandInteraction(event);
        }
    }
}