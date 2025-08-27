package com.discordticketbot.utils;

import com.discordticketbot.config.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ErrorLogger {
    private final Map<String, GuildConfig> guildConfigs;

    public ErrorLogger(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    /**
     * Log error with user context
     */
    public void logError(Guild guild, String operation, String errorMessage, Exception exception, User user) {
        // Delegate to Throwable-based overload to support broader error types
        logError(guild, operation, errorMessage, (Throwable) exception, user, null);
    }

    /**
     * Log error with optional context and Throwable support (new feature, preserves existing format)
     */
    public void logError(Guild guild, String operation, String errorMessage, Throwable throwable, User user, Map<String, String> context) {
        try {
            GuildConfig config = guildConfigs.get(guild.getId());
            if (config == null || config.errorLogChannelId == null) {
                // Fallback to console logging with improved formatting
                System.err.println("═══════════════════════════════════════");
                System.err.println("[ERROR LOG] " + TimestampUtil.getReadableTimestamp());
                System.err.println("Guild: " + guild.getName() + " (" + guild.getId() + ")");
                System.err.println("Operation: " + operation);
                System.err.println("User: " + (user != null ? UserDisplayUtil.getUserLogInfo(user) : "Unknown"));
                System.err.println("Error: " + errorMessage);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                // Optional context dump
                if (context != null && !context.isEmpty()) {
                    System.err.println("Context:");
                    for (Map.Entry<String, String> entry : context.entrySet()) {
                        System.err.println("  - " + entry.getKey() + ": " + entry.getValue());
                    }
                }
                System.err.println("═══════════════════════════════════════");
                return;
            }

            TextChannel errorChannel = guild.getTextChannelById(config.errorLogChannelId);
            if (errorChannel == null) {
                System.err.println("[ERROR] Error log channel not found for guild: " + guild.getName() + " (ID: " + config.errorLogChannelId + ")");
                return;
            }

            // Create detailed error embed with proper formatting
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🚨 Bot Error Report")
                    .setColor(Color.RED)
                    .addField("📅 Timestamp", TimestampUtil.getCurrentTimestampForEmbeds(), true)
                    .addField("🏷️ Operation", operation, true)
                    .addField("👤 User", user != null ? UserDisplayUtil.getFormattedUserInfo(user) : "System/Unknown", true)
                    .addField("🏠 Guild", guild.getName(), true)
                    .addField("❌ Error Message", "```\n" + errorMessage + "\n```", false);

            // New: add root cause and thread/environment details without altering existing primary fields
            if (throwable != null) {
                String stackTrace = getStackTrace(throwable);
                String rootCause = getRootCauseMessage(throwable);
                if (rootCause != null && !rootCause.equals(errorMessage)) {
                    embed.addField("🧩 Root Cause", "```\n" + truncateForField(rootCause) + "\n```", false);
                }
                embed.addField("🧵 Thread", Thread.currentThread().getName(), true)
                     .addField("🖥️ Runtime", System.getProperty("os.name") + " • Java " + System.getProperty("java.version"), true);
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
                    // Also attach full stack trace as a file (new optional feature)
                    byte[] data = stackTrace.getBytes(StandardCharsets.UTF_8);
                    errorChannel.sendMessageEmbeds(embed.build())
                            .addFiles(FileUpload.fromData(data, "stacktrace.txt"))
                            .queue(
                                    success -> {
                                        System.out.println("✅ Error logged to Discord channel with attachment for guild: " + guild.getName());
                                    },
                                    failure -> {
                                        System.err.println("❌ Failed to send error log to Discord: " + failure.getMessage());
                                        System.err.println("[FALLBACK ERROR LOG] " + operation + ": " + errorMessage);
                                        if (throwable != null) {
                                            throwable.printStackTrace();
                                        }
                                    }
                            );
                    return; // Already queued with attachment
                } else {
                    embed.addField("📋 Stack Trace", "```java\n" + stackTrace + "\n```", false);
                }
            }

            // Optional additional context key-values (new feature)
            if (context != null && !context.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : context.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                String contextText = sb.toString();
                if (contextText.length() > 1024) {
                    contextText = contextText.substring(0, 1000) + "...";
                }
                embed.addField("🧭 Context", "```\n" + contextText + "\n```", false);
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
                        if (throwable != null) {
                            throwable.printStackTrace();
                        }
                    }
            );

        } catch (Exception e) {
            // Fallback to console logging if anything fails
            System.err.println("❌ CRITICAL: Error logger failed: " + e.getMessage());
            System.err.println("[ORIGINAL ERROR] " + operation + ": " + errorMessage);
            if (throwable != null) {
                throwable.printStackTrace();
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
     * New overloads to support Throwable and optional context without breaking existing API
     */
    public void logError(Guild guild, String operation, String errorMessage, Throwable throwable) {
        logError(guild, operation, errorMessage, throwable, null, null);
    }

    public void logError(Guild guild, String operation, String errorMessage, Throwable throwable, User user) {
        logError(guild, operation, errorMessage, throwable, user, null);
    }

    public void logErrorWithContext(Guild guild, String operation, String errorMessage, Throwable throwable, User user, Map<String, String> context) {
        logError(guild, operation, errorMessage, throwable, user, context);
    }

    /**
     * Global/server-level error (no guild context). Logs to console with full stack and context.
     */
    public void logGlobalError(String operation, String errorMessage, Throwable throwable, Map<String, String> context) {
        System.err.println("═══════════════════════════════════════");
        System.err.println("[GLOBAL ERROR] " + TimestampUtil.getReadableTimestamp());
        System.err.println("Operation: " + operation);
        System.err.println("Error: " + errorMessage);
        System.err.println("Thread: " + Thread.currentThread().getName());
        System.err.println("Runtime: " + System.getProperty("os.name") + " • Java " + System.getProperty("java.version"));
        if (context != null && !context.isEmpty()) {
            System.err.println("Context:");
            for (Map.Entry<String, String> entry : context.entrySet()) {
                System.err.println("  - " + entry.getKey() + ": " + entry.getValue());
            }
        }
        if (throwable != null) {
            throwable.printStackTrace();
        }
        System.err.println("═══════════════════════════════════════");
    }

    /**
     * Log info/success messages with improved formatting
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
                    .addField("📅 Timestamp", TimestampUtil.getCurrentTimestampForEmbeds(), true)
                    .addField("🏷️ Operation", operation, true)
                    .addField("👤 User", user != null ? UserDisplayUtil.getFormattedUserInfo(user) : "System", true)
                    .addField("ℹ️ Details", message, false)
                    .setFooter("Bot Activity Logger • " + guild.getName(), guild.getIconUrl());

            errorChannel.sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            // Silent failure for info logs
        }
    }

    /**
     * Log successful operations with improved formatting
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
                    .addField("📅 Timestamp", TimestampUtil.getCurrentTimestampForEmbeds(), true)
                    .addField("🏷️ Operation", operation, true)
                    .addField("👤 User", user != null ? UserDisplayUtil.getFormattedUserInfo(user) : "System", true)
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

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message == null || message.isEmpty()) {
            message = root.toString();
        }
        return message;
    }

    private String truncateForField(String input) {
        if (input == null) {
            return "";
        }
        if (input.length() <= 1000) {
            return input;
        }
        return input.substring(0, 1000) + "...";
    }
}