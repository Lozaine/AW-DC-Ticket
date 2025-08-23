package com.discordticketbot.utils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.JDA;

import java.util.EnumSet;

public class BotInviteUtil {

    /**
     * Generate a proper Discord bot invite URL with required permissions and scopes
     * This ensures slash commands will work and appear in bot profile
     */
    public static String generateInviteUrl(JDA jda) {
        if (jda.getSelfUser() == null) {
            return "Bot not ready yet - cannot generate invite URL";
        }

        String clientId = jda.getSelfUser().getId();

        // Required permissions for ticket bot functionality
        EnumSet<Permission> permissions = EnumSet.of(
                Permission.ADMINISTRATOR  // Recommended for full functionality
        );

        // Alternative minimal permissions if Administrator is not desired:
        EnumSet<Permission> minimalPermissions = EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_PERMISSIONS,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_MANAGE,
                Permission.USE_APPLICATION_COMMANDS
        );

        long permissionValue = Permission.getRaw(permissions);

        // Generate invite URL with proper scopes
        String inviteUrl = String.format(
                "https://discord.com/api/oauth2/authorize?client_id=%s&permissions=%d&scope=bot%%20applications.commands",
                clientId,
                permissionValue
        );

        return inviteUrl;
    }

    /**
     * Generate invite URL with minimal permissions
     */
    public static String generateMinimalInviteUrl(JDA jda) {
        if (jda.getSelfUser() == null) {
            return "Bot not ready yet - cannot generate invite URL";
        }

        String clientId = jda.getSelfUser().getId();

        // Minimal permissions for basic ticket functionality
        EnumSet<Permission> minimalPermissions = EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MANAGE_CHANNEL,
                Permission.MANAGE_PERMISSIONS,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_MANAGE,
                Permission.USE_APPLICATION_COMMANDS
        );

        long permissionValue = Permission.getRaw(minimalPermissions);

        String inviteUrl = String.format(
                "https://discord.com/api/oauth2/authorize?client_id=%s&permissions=%d&scope=bot%%20applications.commands",
                clientId,
                permissionValue
        );

        return inviteUrl;
    }

    /**
     * Print invite URLs to console for easy access
     */
    public static void printInviteUrls(JDA jda) {
        System.out.println("\n🔗 Bot Invite URLs:");
        System.out.println("📋 Full permissions (Administrator): " + generateInviteUrl(jda));
        System.out.println("⚡ Minimal permissions: " + generateMinimalInviteUrl(jda));
        System.out.println("\n💡 Important: Use the first URL for best experience!");
        System.out.println("   The 'applications.commands' scope is required for slash commands to appear in bot profile.");
        System.out.println();
    }
}