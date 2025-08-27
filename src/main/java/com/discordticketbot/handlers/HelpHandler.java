package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import com.discordticketbot.utils.HelpSections;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.*;
import java.util.Map;

public class HelpHandler {
    private final Map<String, GuildConfig> guildConfigs; // retained for future guild-specific help

    public HelpHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    public void handle(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = HelpSections.build("overview", event.getJDA().getSelfUser().getAvatarUrl());

        StringSelectMenu menu = StringSelectMenu.create("help_menu")
                .setPlaceholder("Browse help topics…")
                .addOption("Overview", "overview", "Summary of features", Emoji.fromUnicode("📘"))
                .addOption("Administrator Commands", "admin", "/setup, /panel, /config, /stats", Emoji.fromUnicode("👑"))
                .addOption("Staff Commands", "staff", "Close requests, autoclose", Emoji.fromUnicode("🛠️"))
                .addOption("User Features", "user", "Ticket creation & management", Emoji.fromUnicode("👥"))
                .addOption("Database Features", "database", "Persistence & logging", Emoji.fromUnicode("💾"))
                .addOption("Advanced Features", "advanced", "Transcripts, permissions, cleanup", Emoji.fromUnicode("⚙️"))
                .addOption("Quick Setup", "setup", "Step-by-step setup guide", Emoji.fromUnicode("🚀"))
                .build();

        event.replyEmbeds(embed.build())
                .setComponents(ActionRow.of(menu))
                .setEphemeral(true)
                .queue();
    }
}