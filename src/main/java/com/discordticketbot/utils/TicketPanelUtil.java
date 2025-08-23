package com.discordticketbot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

public class TicketPanelUtil {

    public static void createTicketPanel(TextChannel channel) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎫 Support Ticket System")
                .setDescription("Click one of the buttons below to create a new ticket:\n\n" +
                        "🚨 **Report** - Report a user or issue\n" +
                        "💬 **Support** - Get help with general questions\n" +
                        "⚖️ **Appeal** - Appeal a punishment or decision")
                .setColor(Color.GREEN)
                .setFooter("Ticket system powered by our support team");

        channel.sendMessageEmbeds(embed.build())
                .setActionRow(
                        Button.danger("create_report_ticket", "🚨 Report"),
                        Button.primary("create_support_ticket", "💬 Support"),
                        Button.secondary("create_appeal_ticket", "⚖️ Appeal")
                ).queue();
    }
}