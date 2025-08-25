package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.Map;

public class HelpHandler {
    private final Map<String, GuildConfig> guildConfigs;

    public HelpHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    public void handle(SlashCommandInteractionEvent event) {
        EmbedBuilder helpEmbed = new EmbedBuilder()
                .setTitle("🎫 Discord Ticket Bot - Help")
                .setDescription("A comprehensive ticket system with persistent database storage and advanced features.")
                .setColor(Color.BLUE);

        // Administrator Commands
        helpEmbed.addField("👑 Administrator Commands",
                "**`/setup`** - Configure the ticket system\n" +
                        "• Set category, panel channel, transcript channel\n" +
                        "• Configure support roles and error logging\n" +
                        "• Initialize persistent ticket counter\n\n" +
                        "**`/panel`** - Send the ticket creation panel\n" +
                        "• Sends interactive buttons to create tickets\n" +
                        "• Must be used after `/setup`\n\n" +
                        "**`/config`** - View current configuration\n" +
                        "• Shows all configured settings\n" +
                        "• Displays database connection status\n\n" +
                        "**`/stats`** - View ticket statistics\n" +
                        "• Shows total, open, closed tickets\n" +
                        "• Breakdown by ticket types", false);

        // Staff Commands
        helpEmbed.addField("🛡️ Staff Commands",
                "**`/closerequest [reason] [timeout]`** - Request ticket closure\n" +
                        "• Ask ticket owner to confirm closure\n" +
                        "• Optional auto-close timeout (1-168 hours)\n" +
                        "• Requires user confirmation\n\n" +
                        "**`/autoclose exclude`** - Exclude ticket from auto-close\n" +
                        "• Prevents timeout-based closure\n" +
                        "• Useful for complex issues", false);

        // User Features
        helpEmbed.addField("👥 User Features",
                "**Ticket Creation** - Use buttons in panel channel\n" +
                        "• 🎫 Support - General help and questions\n" +
                        "• ⚠️ Report - Report users, bugs, issues\n" +
                        "• ⚖️ Appeal - Appeal punishments\n\n" +
                        "**Ticket Management** - Use buttons in your ticket\n" +
                        "• Close ticket when issue is resolved\n" +
                        "• Only one ticket per user at a time", false);

        // System Features
        helpEmbed.addField("💾 Database Features",
                "**Persistent Storage** - All data saved to PostgreSQL\n" +
                        "• Guild configurations preserved across restarts\n" +
                        "• Ticket counters continue from last number\n" +
                        "• Complete ticket logs and statistics\n" +
                        "• Close request tracking with timeouts", false);

        // Advanced Features
        helpEmbed.addField("⚡ Advanced Features",
                "**Error Logging** - Optional error channel logging\n" +
                        "**Transcript Generation** - Save ticket conversations\n" +
                        "**Permission System** - Role-based access control\n" +
                        "**Auto-close System** - Configurable timeout closures\n" +
                        "**Cleanup Policies** - Automatic log/close-request cleanup\n" +
                        "**Assignments** - Assign tickets to specific staff\n" +
                        "**Close Requests** - User confirmation system", false);

        // Setup Instructions
        helpEmbed.addField("🚀 Quick Setup",
                "1️⃣ Ensure bot has **Administrator** permission\n" +
                        "2️⃣ Use `/setup` to configure channels and roles\n" +
                        "3️⃣ Use `/panel` to send ticket creation buttons\n" +
                        "4️⃣ Users can now create tickets!\n\n" +
                        "**Important:** Bot requires Administrator permission for full functionality.", false);

        helpEmbed.setFooter("Discord Ticket Bot v1.2.0 • Database-backed ticket system",
                        event.getJDA().getSelfUser().getAvatarUrl())
                .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl());

        event.replyEmbeds(helpEmbed.build()).setEphemeral(true).queue();
    }
}