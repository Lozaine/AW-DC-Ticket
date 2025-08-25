package com.discordticketbot.utils;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

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

        OptionData errorLogOpt = new OptionData(OptionType.CHANNEL, "error_log_channel", "🚨 Channel where bot errors will be logged (optional)", false)
                .setChannelTypes(ChannelType.TEXT);

        // Close request options
        OptionData reasonOpt = new OptionData(OptionType.STRING, "reason", "📝 Reason for requesting ticket closure", true);
        OptionData timeoutOpt = new OptionData(OptionType.INTEGER, "timeout", "⏰ Hours until auto-close (optional)", false)
                .setMinValue(1)
                .setMaxValue(168); // 7 days max

        // Cleanup policy options
        OptionData cleanupLogsDaysOpt = new OptionData(OptionType.INTEGER, "logs_days", "🧹 Keep closed/deleted logs for N days (default 30)", false)
                .setMinValue(1)
                .setMaxValue(365);
        OptionData cleanupRequestsDaysOpt = new OptionData(OptionType.INTEGER, "requests_days", "🧹 Keep processed close-requests for N days (default 30)", false)
                .setMinValue(1)
                .setMaxValue(365);
        OptionData transcriptHtmlOpt = new OptionData(OptionType.BOOLEAN, "transcript_html", "🖼️ Also generate HTML transcripts (default on)", false);

        // Assignment command options
        OptionData assignTargetOpt = new OptionData(OptionType.USER, "member", "👤 Staff member to assign this ticket to", true);

        return List.of(
                // Help command - shows all available commands
                Commands.slash("help", "📋 Display all available bot commands and ticket system features"),

                // Setup command - main configuration command
                Commands.slash("setup", "⚙️ Configure the ticket system for this server (Administrator required)")
                        .addOptions(categoryOpt, panelChannelOpt, supportRolesOpt, transcriptOpt, errorLogOpt),

                // Panel command - sends ticket creation panel
                Commands.slash("panel", "🎫 Send the ticket creation panel to configured channel (Administrator required)"),

                // Slash command - view ticket statistics
                Commands.slash("stats", "📊 View ticket statistics for this server (Administrator required)"),

                // Config command - view current settings
                Commands.slash("config", "🔧 View current bot configuration and settings (Administrator required)"),

                // Cleanup config command
                Commands.slash("cleanup", "🧹 Configure automatic cleanup policies (Administrator required)")
                        .addOptions(cleanupLogsDaysOpt, cleanupRequestsDaysOpt, transcriptHtmlOpt),

                // Assignment command
                Commands.slash("assign", "👥 Assign this ticket to a specific staff member (Staff only)")
                        .addOptions(assignTargetOpt),

                // Close request command - request user confirmation to close ticket
                Commands.slash("closerequest", "🔒 Request the ticket owner to confirm closure (Staff only)")
                        .addOptions(reasonOpt, timeoutOpt),

                // Auto-close exclude command - exclude ticket from auto-close timeouts
                Commands.slash("autoclose", "⏰ Manage auto-close settings for this ticket (Staff only)")
                        .addSubcommands(
                                new SubcommandData("exclude", "Exclude a ticket from automatic closure")
                        )
        );
    }
}