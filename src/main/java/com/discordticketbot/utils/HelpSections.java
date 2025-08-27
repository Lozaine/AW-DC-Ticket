package com.discordticketbot.utils;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class HelpSections {
    public static EmbedBuilder build(String section, String avatarUrl) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.BLUE)
                .setThumbnail(avatarUrl)
                .setFooter("Discord Ticket Bot v1.2.0 • Database-backed ticket system", avatarUrl);

        switch (section) {
            case "admin" -> {
                embed.setTitle("👑 Administrator Commands");
                embed.setDescription("Commands for configuring and managing the system.");
                embed.addField("/setup",
                        "Configure ticket system: categories, panel/transcript channels, roles, error logging, counters.", false);
                embed.addField("/panel",
                        "Post the interactive ticket creation panel. Use after `/setup`.", false);
                embed.addField("/config",
                        "View all configured settings and database connection status.", false);
                embed.addField("/stats",
                        "Show ticket statistics: totals, open/closed, and per-type breakdown.", false);
            }
            case "staff" -> {
                embed.setTitle("🛠️ Staff Commands");
                embed.setDescription("Tools for staff operating tickets.");
                embed.addField("/closerequest [reason] [timeout]",
                        "Request ticket closure with optional auto-close (1-168h). Requires user confirmation.", false);
                embed.addField("/autoclose exclude",
                        "Exclude a ticket from auto-close timers. Useful for complex issues.", false);
            }
            case "user" -> {
                embed.setTitle("👥 User Features");
                embed.addField("Ticket Creation",
                        "Use the panel buttons to create: 🎫 Support, ⚠️ Report, ⚖️ Appeal.", false);
                embed.addField("Ticket Management",
                        "Manage your ticket via buttons. Close when resolved. One ticket per user at a time.", false);
            }
            case "database" -> {
                embed.setTitle("💾 Database Features");
                embed.addField("Persistent Storage",
                        "All data saved to PostgreSQL: guild configs, counters, logs, stats, close requests.", false);
            }
            case "advanced" -> {
                embed.setTitle("⚙️ Advanced Features");
                embed.setDescription("Additional capabilities for robust operations.");
                embed.addField("Error Logging", "Optional error channel logging.", false);
                embed.addField("Transcript Generation", "Create and store ticket conversation transcripts.", false);
                embed.addField("Permission System", "Role-based access control.", false);
                embed.addField("Auto-close System", "Configurable timeout closures.", false);
                embed.addField("Cleanup Policies", "Automatic log and close-request cleanup.", false);
                embed.addField("Assignments", "Assign tickets to specific staff.", false);
                embed.addField("Close Requests", "User confirmation system.", false);
            }
            case "setup" -> {
                embed.setTitle("🚀 Quick Setup");
                embed.setDescription("Step-by-step to get started quickly.");
                embed.addField("1️⃣ Permissions", "Ensure bot has Administrator permission.", false);
                embed.addField("2️⃣ Configure", "Run `/setup` to set channels and roles.", false);
                embed.addField("3️⃣ Panel", "Run `/panel` to post ticket creation buttons.", false);
                embed.addField("4️⃣ Go!", "Users can now create tickets.", false);
                embed.addField("Important", "Administrator permission is required for full functionality.", false);
            }
            case "overview" -> {
                embed.setTitle("🎫 Discord Ticket Bot - Help");
                embed.setDescription("A comprehensive ticket system with persistent storage and advanced features.");
                embed.addField("Key Areas",
                        "Use the dropdown below to browse: Admin, Staff, User, Database, Advanced, Setup.", false);
            }
            default -> {
                embed.setTitle("🎫 Help");
                embed.setDescription("Select a topic from the dropdown below.");
            }
        }

        return embed;
    }
}


