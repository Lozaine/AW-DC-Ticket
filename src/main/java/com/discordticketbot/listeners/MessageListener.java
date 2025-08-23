package com.discordticketbot.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

public class MessageListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        if (message.startsWith("!")) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🔄 Command System Updated!")
                    .setDescription("This bot now uses **slash commands** instead of text commands!\n\n" +
                            "Use `/help` to see all available commands.")
                    .setColor(Color.CYAN)
                    .setFooter("Slash commands are more secure and user-friendly!");
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        }
    }
}