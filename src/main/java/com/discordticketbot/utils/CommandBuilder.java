package com.discordticketbot.utils;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class CommandBuilder {

    public static List<CommandData> buildCommands() {
        OptionData categoryOpt = new OptionData(OptionType.CHANNEL, "category", "Category for ticket channels", true)
                .setChannelTypes(ChannelType.CATEGORY);
        OptionData transcriptOpt = new OptionData(OptionType.CHANNEL, "transcript_channel", "Channel for transcripts", true)
                .setChannelTypes(ChannelType.TEXT);
        OptionData panelChannelOpt = new OptionData(OptionType.CHANNEL, "panel_channel", "Channel for ticket panel", true)
                .setChannelTypes(ChannelType.TEXT);

        // STRING: accept multiple role mentions/IDs
        OptionData supportRolesOpt = new OptionData(
                OptionType.STRING,
                "support_roles",
                "Support roles as @mentions or IDs (space/comma-separated)",
                true
        );

        return List.of(
                Commands.slash("help", "Show all available commands"),
                Commands.slash("setup", "Configure the ticket system for this server (Admin only)")
                        .addOptions(categoryOpt, panelChannelOpt, supportRolesOpt, transcriptOpt),
                Commands.slash("panel", "Send the ticket panel to configured channel (Admin only)"),
                Commands.slash("config", "View current bot configuration (Admin only)")
        );
    }
}