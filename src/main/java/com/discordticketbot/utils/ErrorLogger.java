package com.discordticketbot.utils;

import com.discordticketbot.config.GuildConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;

public class ErrorLogger {
    private final Map<String, GuildConfig> guildConfigs;

    public ErrorLogger(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
    }

    public void logError(Guild guild, String operation, String errorMessage, Exception exception) {
        try {
            GuildConfig config = guildConfigs.get(guild.getId());
            if (config == null || config.errorLogChannelId == null) {
                // Fallback to console logging
                System.err.println("[ERROR] Guild: " + guild.getName() + " | Operation: " + operation + " | Error: " + errorMessage);
                if (exception != null) {
                    exception.printStackTrace();
                }
                return;
            }

            TextChannel errorChannel = guild.getTextChannelById(config.errorLogChannelId);
            if (errorChannel == null) {
                System.err.println("[ERROR] Error log channel not found for guild: " + guild.getName());
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🚨 Bot Error Log")
                    .setDescription("**Operation:** " + operation + "\n**Error:** " + errorMessage)
                    .setColor(Color.RED)
                    .setTimestamp(Instant.now())
                    .setFooter("Error Logger", guild.getJDA().getSelfUser().getAvatarUrl());

            if (exception != null) {
                String stackTrace = getStackTrace(exception);
                if (stackTrace.length() > 1024) {
                    stackTrace = stackTrace.substring(0, 1021) + "...";
                }
                embed.addField("📄 Stack Trace", "```\n" + stackTrace + "\n```", false);
            }

            errorChannel.sendMessageEmbeds(embed.build()).queue(
                    success -> {}, // Success - do nothing
                    failure -> {
                        // Fallback to console if Discord fails
                        System.err.println("[ERROR] Failed to send error log to Discord: " + failure.getMessage());
                        System.err.println("[ORIGINAL ERROR] " + operation + ": " + errorMessage);
                        if (exception != null) {
                            exception.printStackTrace();
                        }
                    }
            );

        } catch (Exception e) {
            // Fallback to console logging if anything fails
            System.err.println("[CRITICAL ERROR] Error logger failed: " + e.getMessage());
            System.err.println("[ORIGINAL ERROR] " + operation + ": " + errorMessage);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }

    public void logInfo(Guild guild, String operation, String message) {
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
                    .setTitle("ℹ️ Bot Info Log")
                    .setDescription("**Operation:** " + operation + "\n**Info:** " + message)
                    .setColor(Color.BLUE)
                    .setTimestamp(Instant.now())
                    .setFooter("Info Logger", guild.getJDA().getSelfUser().getAvatarUrl());

            errorChannel.sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            // Silent failure for info logs
        }
    }

    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}