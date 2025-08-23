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

        // Register slash commands GLOBALLY ONLY
        // This makes them appear in the bot profile with {/} buttons and commands list
        event.getJDA().updateCommands().addCommands(CommandBuilder.buildCommands()).queue(
                success -> {
                    System.out.println("✅ Successfully registered " + success.size() + " global slash commands");
                    System.out.println("🎯 Commands will appear in bot profile in 1-2 minutes");
                    System.out.println("📋 Registered commands:");
                    success.forEach(cmd -> System.out.println("   /" + cmd.getName()));
                },
                error -> {
                    System.err.println("❌ Failed to register slash commands: " + error.getMessage());
                    error.printStackTrace();
                }
        );
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        // No need to register per-guild commands since we're using global commands
        System.out.println("🎉 Joined guild: " + event.getGuild().getName() + " (Global commands already available)");
    }
}