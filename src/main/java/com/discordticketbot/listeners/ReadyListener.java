package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.CommandBuilder;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;
import java.util.Map;

public class ReadyListener extends ListenerAdapter {
    private final Map<String, GuildConfig> guildConfigs;

    public ReadyListener(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("🤖 Bot logged in as: " + event.getJDA().getSelfUser().getAsTag());
        System.out.println("🏠 Connected to " + event.getJDA().getGuilds().size() + " guilds");

        // Register global slash commands
        try {
            System.out.println("🔄 Registering global slash commands...");
            List<CommandData> commands = CommandBuilder.buildCommands();

            event.getJDA().updateCommands().addCommands(commands).queue(
                    success -> {
                        System.out.println("✅ Successfully registered " + commands.size() + " global slash commands!");
                        System.out.println("📋 Registered commands:");
                        for (CommandData cmd : commands) {
                            System.out.println("   • /" + cmd.getName() + " - " + cmd.getName() + " command");
                        }
                        System.out.println("⏰ Commands may take up to 1 hour to appear globally");
                    },
                    failure -> {
                        System.err.println("❌ Failed to register global commands: " + failure.getMessage());
                        failure.printStackTrace();
                    }
            );
        } catch (Exception e) {
            System.err.println("❌ Error during command registration: " + e.getMessage());
            e.printStackTrace();
        }

        // Log loaded guild configurations
        System.out.println("📊 Guild Configuration Status:");
        for (Map.Entry<String, GuildConfig> entry : guildConfigs.entrySet()) {
            String guildId = entry.getKey();
            GuildConfig config = entry.getValue();
            String guildName = event.getJDA().getGuildById(guildId) != null ?
                    event.getJDA().getGuildById(guildId).getName() : "Unknown Guild";

            System.out.println("   • " + guildName + " (" + guildId + "): " +
                    (config.isConfigured() ? "✅ Configured" : "❌ Not Configured"));
        }

        System.out.println("🎫 Discord Ticket Bot is ready to handle tickets!");
    }
}