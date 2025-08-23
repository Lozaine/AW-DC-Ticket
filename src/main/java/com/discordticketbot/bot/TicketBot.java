package com.discordticketbot.bot;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.listeners.CommandListener;
import com.discordticketbot.listeners.ButtonListener;
import com.discordticketbot.listeners.MessageListener;
import com.discordticketbot.listeners.ReadyListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import java.util.HashMap;
import java.util.Map;

public class TicketBot {
    private final String botToken;
    private JDA jda;
    private final Map<String, GuildConfig> guildConfigs = new HashMap<>();

    public TicketBot(String botToken) {
        this.botToken = botToken;
    }

    public void start() throws Exception {
        jda = JDABuilder.createDefault(botToken)
                .addEventListeners(
                        new ReadyListener(jda, guildConfigs),
                        new CommandListener(guildConfigs),
                        new ButtonListener(guildConfigs),
                        new MessageListener()
                )
                .build();
    }

    public JDA getJda() {
        return jda;
    }

    public Map<String, GuildConfig> getGuildConfigs() {
        return guildConfigs;
    }
}