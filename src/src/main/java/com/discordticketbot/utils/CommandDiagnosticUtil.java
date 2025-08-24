package com.discordticketbot.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command;

public class CommandDiagnosticUtil {

    /**
     * Print diagnostic information about registered commands
     */
    public static void printCommandDiagnostics(JDA jda) {
        System.out.println("\n🔍 COMMAND DIAGNOSTICS");
        System.out.println("═".repeat(40));

        // Check global commands
        jda.retrieveCommands().queue(
                globalCommands -> {
                    System.out.println("🌍 Global Commands Found: " + globalCommands.size());

                    if (globalCommands.isEmpty()) {
                        System.out.println("   ❌ No global commands registered!");
                        System.out.println("   💡 Commands won't appear in bot profile");
                    } else {
                        System.out.println("   ✅ Commands registered:");
                        for (Command cmd : globalCommands) {
                            System.out.println("      • /" + cmd.getName() + " (ID: " + cmd.getId() + ")");
                        }
                    }

                    System.out.println("\n📊 Status Summary:");
                    System.out.println("   • Bot ID: " + jda.getSelfUser().getId());
                    System.out.println("   • Expected commands: 4 (help, setup, panel, config)");
                    System.out.println("   • Actual commands: " + globalCommands.size());

                    if (globalCommands.size() == 4) {
                        System.out.println("   ✅ Perfect! Commands should appear in profile");
                    } else if (globalCommands.size() > 0) {
                        System.out.println("   ⚠️  Partial registration - profile may be incomplete");
                    } else {
                        System.out.println("   ❌ No commands - profile Commands section missing");
                    }

                    System.out.println("═".repeat(40));
                },
                error -> {
                    System.err.println("❌ Failed to check commands: " + error.getMessage());
                    System.out.println("   This may indicate registration issues");
                }
        );
    }

    /**
     * Print optimized bot invite URL for command visibility
     */
    public static void printOptimizedInviteUrl(JDA jda) {
        if (jda.getSelfUser() == null) {
            System.out.println("⏳ Bot not ready - cannot generate invite URL yet");
            return;
        }

        String botId = jda.getSelfUser().getId();
        String optimizedUrl = String.format(
                "https://discord.com/oauth2/authorize?client_id=%s&permissions=8&scope=bot%%20applications.commands",
                botId
        );

        System.out.println("\n🔗 OPTIMIZED INVITE URL:");
        System.out.println(optimizedUrl);
        System.out.println("\n💡 This URL ensures:");
        System.out.println("   • ✅ applications.commands scope");
        System.out.println("   • ✅ Administrator permissions");
        System.out.println("   • ✅ Proper bot integration");
        System.out.println("   • ✅ Commands appear in profile");
    }
}