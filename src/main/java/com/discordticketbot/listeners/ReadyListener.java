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
        // Clear global commands to avoid global registration delays
        jda.updateCommands().addCommands().queue();
        for (Guild g : jda.getGuilds()) {
            registerCommandsForGuild(g);
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        registerCommandsForGuild(event.getGuild());
    }

    private void registerCommandsForGuild(Guild guild) {
        guild.updateCommands().addCommands(CommandBuilder.buildCommands()).queue(
                v -> System.out.println("Registered commands in guild: " + guild.getName()),
                err -> System.err.println("Failed to register commands in " + guild.getName() + ": " + err)
        );
    }
}