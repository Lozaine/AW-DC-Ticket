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

        System.out.println("🤖 Bot is ready! Registering slash commands...");

        // Register commands globally so they appear in bot's profile
        jda.updateCommands().addCommands(CommandBuilder.buildCommands()).queue(
                success -> {
                    System.out.println("✅ Global slash commands registered successfully!");
                    System.out.println("📋 Commands will appear in bot profile with {/} buttons");
                    System.out.println("⏰ Note: Global commands may take up to 1 hour to sync across all servers");
                },
                error -> {
                    System.err.println("❌ Failed to register global commands: " + error.getMessage());
                    error.printStackTrace();

                    // Fallback to guild-specific registration
                    System.out.println("🔄 Falling back to guild-specific command registration...");
                    for (Guild guild : jda.getGuilds()) {
                        registerCommandsForGuild(guild);
                    }
                }
        );
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        System.out.println("🆕 Joined new guild: " + event.getGuild().getName());
        // Global commands will automatically be available in new guilds
        // But we can also register guild-specific for immediate availability
        registerCommandsForGuild(event.getGuild());
    }

    /**
     * Register commands for a specific guild (faster than global, but doesn't show in profile)
     * Use this as fallback or for immediate testing
     */
    private void registerCommandsForGuild(Guild guild) {
        guild.updateCommands().addCommands(CommandBuilder.buildCommands()).queue(
                success -> System.out.println("✅ Guild commands registered for: " + guild.getName()),
                error -> System.err.println("❌ Failed to register commands in " + guild.getName() + ": " + error.getMessage())
        );
    }
}