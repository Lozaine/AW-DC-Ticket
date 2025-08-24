package com.discordticketbot.listeners;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.utils.CommandBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;
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
        System.out.println("🔧 Bot ID: " + event.getJDA().getSelfUser().getId());

        // Register commands with proper error handling
        registerGlobalCommands();

        // Print helpful information
        printBotInfo();
    }

    /**
     * Register commands globally for bot profile visibility
     */
    private void registerGlobalCommands() {
        List<CommandData> commands = CommandBuilder.buildCommands();

        System.out.println("\n🔄 Registering global slash commands...");

        // Clear existing commands first
        jda.updateCommands().queue(
                cleared -> {
                    System.out.println("✅ Cleared existing commands");

                    // Register new commands
                    jda.updateCommands().addCommands(commands).queue(
                            success -> {
                                System.out.println("✅ Successfully registered " + success.size() + " global commands:");
                                System.out.println("   🔸 /help - Show help information");
                                System.out.println("   🔸 /setup - Configure ticket system");
                                System.out.println("   🔸 /panel - Send ticket panel");
                                System.out.println("   🔸 /config - View configuration");

                                System.out.println("\n📋 Commands are now:");
                                System.out.println("   ✅ Registered globally");
                                System.out.println("   ✅ Available via typing /");
                                System.out.println("   ✅ Should appear in bot profile");

                                // Also register to current guilds for immediate availability
                                registerToCurrentGuilds();
                            },
                            error -> {
                                System.err.println("❌ Failed to register global commands: " + error.getMessage());
                                // Fallback: register to guilds individually
                                registerToCurrentGuilds();
                            }
                    );
                },
                error -> {
                    System.err.println("❌ Failed to clear commands: " + error.getMessage());
                    // Continue with registration anyway
                    jda.updateCommands().addCommands(commands).queue(
                            success -> System.out.println("✅ Registered " + success.size() + " commands"),
                            failure -> System.err.println("❌ Command registration failed: " + failure.getMessage())
                    );
                }
        );
    }

    /**
     * Register commands to current guilds for immediate testing
     */
    private void registerToCurrentGuilds() {
        System.out.println("🧪 Registering to current guilds for immediate testing...");
        List<CommandData> commands = CommandBuilder.buildCommands();

        for (Guild guild : jda.getGuilds()) {
            guild.updateCommands().addCommands(commands).queue(
                    success -> System.out.println("   ✅ " + guild.getName()),
                    error -> System.out.println("   ❌ " + guild.getName() + ": " + error.getMessage())
            );
        }
    }

    /**
     * Print bot information and profile details
     */
    private void printBotInfo() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📋 BOT PROFILE & COMMANDS INFORMATION");
        System.out.println("=".repeat(60));
        System.out.println("🤖 Bot Name: " + jda.getSelfUser().getName());
        System.out.println("🆔 Bot ID: " + jda.getSelfUser().getId());
        System.out.println("🔗 Profile: https://discord.com/users/" + jda.getSelfUser().getId());

        System.out.println("\n📋 Expected Commands in Profile:");
        System.out.println("   🔸 help - Display bot help and features");
        System.out.println("   🔸 setup - Configure ticket system (Admin only)");
        System.out.println("   🔸 panel - Send ticket creation panel (Admin only)");
        System.out.println("   🔸 config - View current configuration (Admin only)");

        System.out.println("\n⏰ Timeline for Commands Section:");
        System.out.println("   • Now: Commands work when typed /");
        System.out.println("   • 1-5 min: {/} buttons appear ✅");
        System.out.println("   • 5-60 min: Commands list in profile");
        System.out.println("   • Max 24h: Full global propagation");

        System.out.println("\n🔍 Troubleshooting:");
        System.out.println("   • Type / in Discord to test commands");
        System.out.println("   • Check profile in different servers");
        System.out.println("   • Wait up to 1 hour for Commands section");
        System.out.println("   • Try refreshing Discord client");

        System.out.println("\n🔗 Invite URL with proper scopes:");
        String inviteUrl = String.format(
                "https://discord.com/oauth2/authorize?client_id=%s&permissions=8&scope=bot%%20applications.commands",
                jda.getSelfUser().getId()
        );
        System.out.println(inviteUrl);
        System.out.println("=".repeat(60));
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        System.out.println("🆕 Joined guild: " + event.getGuild().getName());

        // Register commands to this guild for immediate availability
        event.getGuild().updateCommands().addCommands(CommandBuilder.buildCommands()).queue(
                success -> System.out.println("   ✅ Commands registered to " + event.getGuild().getName()),
                error -> System.out.println("   ❌ Failed to register commands to " + event.getGuild().getName())
        );
    }
}