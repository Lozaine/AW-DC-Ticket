package com.discordticketbot.utils;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class CommandBuilder {

    public static List<CommandData> buildCommands() {
        // Channel options with proper descriptions and emojis
        OptionData categoryOpt = new OptionData(OptionType.CHANNEL, "category", "📁 Category where ticket channels will be created", true)
                .setChannelTypes(ChannelType.CATEGORY);

        OptionData transcriptOpt = new OptionData(OptionType.CHANNEL, "transcript_channel", "📜 Channel where ticket transcripts will be saved", true)
                .setChannelTypes(ChannelType.TEXT);

        OptionData panelChannelOpt = new OptionData(OptionType.CHANNEL, "panel_channel", "📋 Channel where the ticket panel will be displayed", true)
                .setChannelTypes(ChannelType.TEXT);

        // Support roles option with detailed description and emoji
        OptionData supportRolesOpt = new OptionData(
                OptionType.STRING,
                "support_roles",
                "👥 Staff roles that can manage tickets (@Staff @Mods or role IDs)",
                true
        );

        return List.of(
                // Help command - shows all available commands
                Commands.slash("help", "📋 Display all available bot commands and ticket system features"),

                // Setup command - main configuration command
                Commands.slash("setup", "⚙️ Configure the ticket system for this server (Administrator required)")
                        .addOptions(categoryOpt, panelChannelOpt, supportRolesOpt, transcriptOpt),

                // Panel command - sends ticket creation panel
                Commands.slash("panel", "🎫 Send the ticket creation panel to configured channel (Administrator required)"),

                // Slash command - view ticket statistics
                Commands.slash("stats", "📊 View ticket statistics for this server (Administrator required)"),

                // Config command - view current settings
                Commands.slash("config", "🔧 View current bot configuration and settings (Administrator required)")
        );
    }
}