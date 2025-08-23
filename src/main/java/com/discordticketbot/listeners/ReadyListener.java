package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.CommandBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Map;

public class ReadyListener extends ListenerAdapter {
    private JDA jda;
    private final Map<String, GuildConfig> guildConfigs;

    public ReadyListener(JDA jda, Map<String, GuildConfig> guildConfigs) {
        this.jda = jda;
        this.guildConfigs = guildConfigs;
    }

    @Override
    public void onReady(ReadyEvent event) {
        this.jda = event.getJDA();

        System.out.println("🤖 Bot is ready! Logged in as: " + event.getJDA().getSelfUser().getName());
        System.out.println("📊 Connected to " + event.getGuildTotalCount() + " servers");

        // ONLY register commands globally - this makes them appear in bot profile
        // Clear any existing commands first to prevent conflicts
        System.out.println("🧹 Clearing any existing global commands...");
        event.getJDA().updateCommands().queue(
                cleared -> {
                    System.out.println("✅ Cleared existing commands, registering new ones...");

                    // Now register our commands globally
                    event.getJDA().updateCommands().addCommands(CommandBuilder.buildCommands()).queue(
                            success -> {
                                System.out.println("✅ Successfully registered " + success.size() + " global slash commands");
                                System.out.println("📋 Commands registered:");
                                success.forEach(cmd -> System.out.println("   🔸 /" + cmd.getName() + " - " + cmd.getDescription()));
                                System.out.println("\n🎯 Commands will appear in bot profile within 1-60 minutes");
                                System.out.println("💡 You can test them immediately by typing / in Discord");
                            },
                            error -> {
                                System.err.println("❌ Failed to register global commands: " + error.getMessage());
                                error.printStackTrace();
                            }
                    );
                },
                error -> {
                    System.err.println("❌ Failed to clear existing commands: " + error.getMessage());
                    error.printStackTrace();
                }
        );
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        // No guild-specific registration needed - global commands work everywhere
        System.out.println("🆕 Joined guild: " + event.getGuild().getName() + " (Global commands available)");
    }
}