
package com.discordticketbot.utils;

import com.discordticketbot.config.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ErrorLogger {
    private final Map<String, GuildConfig> guildConfigs;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ErrorLogger(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    /**
     * Log error with user context
     */
    public void logError(Guild guild, String operation, String errorMessage, Exception exception, User user) {
        try {
            GuildConfig config = guildConfigs.get(guild.getId());
            if (config == null || config.errorLogChannelId == null) {
                // Fallback to console logging with detailed info
                System.err.println("═══════════════════════════════════════");
                System.err.println("[ERROR LOG] " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
                System.err.println("Guild: " + guild.getName() + " (" + guild.getId() + ")");
                System.err.println("Operation: " + operation);
                System.err.println("User: " + (user != null ? user.getAsTag() + " (" + user.getId() + ")" : "Unknown"));
                System.err.println("Error: " + errorMessage);
                if (exception != null) {
                    exception.printStackTrace();
                }
                System.err.println("═══════════════════════════════════════");
                return;
            }

            TextChannel errorChannel = guild.getTextChannelById(config.errorLogChannelId);
            if (errorChannel == null) {
                System.err.println("[ERROR] Error log channel not found for guild: " + guild.getName() + " (ID: " + config.errorLogChannelId + ")");
                return;
            }

            // Create detailed error embed in transcript style
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🚨 Bot Error Report")
                    .setColor(Color.RED)
                    .addField("📅 Timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT) + " UTC+08", true)
                    .addField("🏷️ Operation", operation, true)
                    .addField("👤 User", user != null ? user.getAsMention() + " (" + user.getAsTag() + ")" : "System/Unknown", true)
                    .addField("🏠 Guild", guild.getName(), true)
                    .addField("❌ Error Message", "```\n" + errorMessage + "\n```", false);

            if (exception != null) {
                String stackTrace = getStackTrace(exception);
                // Split stack trace if too long
                if (stackTrace.length() > 1024) {
                    embed.addField("📋 Stack Trace (Part 1)", "```java\n" + stackTrace.substring(0, 1000) + "...\n```", false);
                    if (stackTrace.length() > 1000) {
                        String remaining = stackTrace.substring(1000);
                        if (remaining.length() > 1024) {
                            remaining = remaining.substring(0, 1000) + "...";
                        }
                        embed.addField("📋 Stack Trace (Part 2)", "```java\n" + remaining + "\n```", false);
                    }
                } else {
                    embed.addField("📋 Stack Trace", "```java\n" + stackTrace + "\n```", false);
                }
            }

            embed.setFooter("Bot Error Logger • " + guild.getName(), guild.getIconUrl());

            errorChannel.sendMessageEmbeds(embed.build()).queue(
                    success -> {
                        System.out.println("✅ Error logged to Discord channel for guild: " + guild.getName());
                    },
                    failure -> {
                        // Fallback to console if Discord fails
                        System.err.println("❌ Failed to send error log to Discord: " + failure.getMessage());
                        System.err.println("[FALLBACK ERROR LOG] " + operation + ": " + errorMessage);
                        if (exception != null) {
                            exception.printStackTrace();
                        }
                    }
            );

        } catch (Exception e) {
            // Fallback to console logging if anything fails
            System.err.println("❌ CRITICAL: Error logger failed: " + e.getMessage());
            System.err.println("[ORIGINAL ERROR] " + operation + ": " + errorMessage);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Log error without user context
     */
    public void logError(Guild guild, String operation, String errorMessage, Exception exception) {
        logError(guild, operation, errorMessage, exception, null);
    }

    /**
     * Log info/success messages in transcript style
     */
    public void logInfo(Guild guild, String operation, String message, User user) {
        try {
            GuildConfig config = guildConfigs.get(guild.getId());
            if (config == null || config.errorLogChannelId == null) {
                return; // Skip info logs if no channel configured
            }

            TextChannel errorChannel = guild.getTextChannelById(config.errorLogChannelId);
            if (errorChannel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("ℹ️ Bot Activity Log")
                    .setColor(Color.BLUE)
                    .addField("📅 Timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT) + " UTC+08", true)
                    .addField("🏷️ Operation", operation, true)
                    .addField("👤 User", user != null ? user.getAsMention() + " (" + user.getAsTag() + ")" : "System", true)
                    .addField("ℹ️ Details", message, false)
                    .setFooter("Bot Activity Logger • " + guild.getName(), guild.getIconUrl());

            errorChannel.sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            // Silent failure for info logs
        }
    }

    /**
     * Log successful operations
     */
    public void logSuccess(Guild guild, String operation, String message, User user) {
        try {
            GuildConfig config = guildConfigs.get(guild.getId());
            if (config == null || config.errorLogChannelId == null) {
                return;
            }

            TextChannel errorChannel = guild.getTextChannelById(config.errorLogChannelId);
            if (errorChannel == null) {
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("✅ Bot Success Log")
                    .setColor(Color.GREEN)
                    .addField("📅 Timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT) + " UTC+08", true)
                    .addField("🏷️ Operation", operation, true)
                    .addField("👤 User", user != null ? user.getAsMention() + " (" + user.getAsTag() + ")" : "System", true)
                    .addField("✅ Result", message, false)
                    .setFooter("Bot Success Logger • " + guild.getName(), guild.getIconUrl());

            errorChannel.sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            // Silent failure for success logs
        }
    }

    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}
