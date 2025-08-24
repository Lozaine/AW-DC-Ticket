package com.discordticketbot.handlers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class HelpHandler {

    public void handle(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎫 Ticket Bot Commands")
                .setDescription("Here are all available commands for the ticket system:")
                .addField("📋 General Commands",
                        "`/help` - Show this help message", false)
                .addField("⚙️ Admin Commands",
                        "`/setup` - Configure the ticket system\n" +
                                "`/panel` - Send ticket panel to configured channel\n" +
                                "`/config` - View current configuration", false)
                .addField("🎫 Ticket Features",
                        "🚨 **Report** - Report users or issues\n" +
                                "💬 **Support** - Get help with general questions\n" +
                                "⚖️ **Appeal** - Appeal punishments or decisions", false)
                .addField("🔧 Bot Features",
                        "• Automatic permission management\n" +
                                "• Transcript generation\n" +
                                "• Close/Reopen system\n" +
                                "• Staff role integration", false)
                .addField("🔒 Required Permissions",
                        "**Administrator permission required** for:\n" +
                                "• Dynamic channel creation and management\n" +
                                "• Complex permission overrides\n" +
                                "• Reliable transcript generation\n" +
                                "• Cross-category operations", false)
                .addField("🔒 `/closerequest <reason> [timeout]`",
                        "Request the ticket owner to confirm closure with optional auto-close timeout (Staff only)", false)
                .addField("🔒 Close Ticket Button",
                        "Close ticket with optional reason - shows modal to enter close reason (Owner/Staff)", false)
                .addField("🛡️ Security Note",
                        "Administrator permission ensures the ticket system works reliably.\n" +
                                "The bot only uses these permissions for ticket operations.", false)
                .setColor(Color.GREEN)
                .setFooter("Use slash commands (/) for the best experience!");
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}