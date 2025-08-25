package com.discordticketbot.handlers;

import com.discordticketbot.config.GuildConfig;
import com.discordticketbot.database.TicketLogDAO;
import com.discordticketbot.utils.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class StatsHandler {
    private final Map<String, GuildConfig> guildConfigs;
    private final TicketLogDAO ticketLogDAO;

    public StatsHandler(Map<String, GuildConfig> guildConfigs) {
        this.guildConfigs = guildConfigs;
        this.ticketLogDAO = new TicketLogDAO();
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasAdminPermission(event.getMember())) {
            event.reply("❌ You need Administrator permissions to use this command.")
                    .setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ This command can only be used in a server.")
                    .setEphemeral(true).queue();
            return;
        }

        GuildConfig config = guildConfigs.get(guild.getId());
        if (config == null) {
            event.reply("❌ Ticket system is not configured. Please use `/setup` first.")
                    .setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue(); // Defer reply as database queries may take time

        try {
            // Get ticket statistics
            Map<String, Integer> stats = ticketLogDAO.getTicketStats(guild.getId());
            Map<String, Integer> typeStats = ticketLogDAO.getTicketStatsByType(guild.getId());
            List<TicketLogDAO.TicketLog> recentActivity = ticketLogDAO.getRecentTicketActivity(guild.getId(), 10);

            EmbedBuilder statsEmbed = new EmbedBuilder()
                    .setTitle("📊 Ticket Statistics")
                    .setDescription("Comprehensive ticket system statistics for **" + guild.getName() + "**")
                    .setColor(Color.BLUE)
                    .setThumbnail(guild.getIconUrl());

            // Overall statistics
            int total = stats.getOrDefault("total", 0);
            int open = stats.getOrDefault("open", 0);
            int closed = stats.getOrDefault("closed", 0);
            int deleted = stats.getOrDefault("deleted", 0);
            int autoClosed = stats.getOrDefault("auto_closed", 0);
            int reopened = stats.getOrDefault("reopened", 0);

            statsEmbed.addField("📈 Overall Statistics",
                    "**Total Tickets:** " + total + "\n" +
                            "**Open:** " + open + " 🟢\n" +
                            "**Closed:** " + closed + " 🔴\n" +
                            "**Deleted:** " + deleted + " 🗑️\n" +
                            "**Auto-closed:** " + autoClosed + " ⏰\n" +
                            "**Reopened:** " + reopened + " 🔄", true);

            // Current system status
            statsEmbed.addField("🎯 System Status",
                    "**Ticket Counter:** " + config.ticketCounter + "\n" +
                            "**Next Ticket:** #" + String.format("%03d", config.ticketCounter + 1) + "\n" +
                            "**Database:** ✅ Connected\n" +
                            "**Configuration:** " + (config.isConfigured() ? "✅ Complete" : "❌ Incomplete"), true);

            // Ticket types breakdown
            if (!typeStats.isEmpty()) {
                StringBuilder typeBreakdown = new StringBuilder();
                int supportTickets = typeStats.getOrDefault("Support", 0);
                int reportTickets = typeStats.getOrDefault("Report", 0);
                int appealTickets = typeStats.getOrDefault("Appeal", 0);

                typeBreakdown.append("🎫 **Support:** ").append(supportTickets).append("\n");
                typeBreakdown.append("⚠️ **Report:** ").append(reportTickets).append("\n");
                typeBreakdown.append("⚖️ **Appeal:** ").append(appealTickets).append("\n");

                // Calculate percentages if there are tickets
                if (total > 0) {
                    typeBreakdown.append("\n**Percentages:**\n");
                    typeBreakdown.append("Support: ").append(Math.round((supportTickets * 100.0) / total)).append("%\n");
                    typeBreakdown.append("Report: ").append(Math.round((reportTickets * 100.0) / total)).append("%\n");
                    typeBreakdown.append("Appeal: ").append(Math.round((appealTickets * 100.0) / total)).append("%");
                }

                statsEmbed.addField("📋 Ticket Types", typeBreakdown.toString(), true);
            }

            // Activity summary
            if (total > 0) {
                double closureRate = ((double) (closed + deleted + autoClosed) / total) * 100;
                double reopenRateValue = reopened > 0 ? ((double) reopened / (closed + deleted)) * 100 : 0;

                statsEmbed.addField("📊 Performance Metrics",
                        "**Closure Rate:** " + String.format("%.1f", closureRate) + "%\n" +
                                "**Reopen Rate:** " + String.format("%.1f", reopenRateValue) + "%\n" +
                                "**Active Tickets:** " + open + "\n" +
                                "**Resolution Status:** " + (open == 0 ? "✅ All Clear" :
                                open <= 5 ? "🟡 Manageable" : "🔴 High Volume"), false);
            }

            // Recent activity
            if (!recentActivity.isEmpty()) {
                StringBuilder recentText = new StringBuilder();
                int displayCount = Math.min(5, recentActivity.size());

                for (int i = 0; i < displayCount; i++) {
                    TicketLogDAO.TicketLog log = recentActivity.get(i);
                    String emoji = getStatusEmoji(log.status);
                    String timeAgo = getTimeAgo(log.createdAt.getTime());
                    recentText.append(emoji).append(" **").append(log.channelName).append("** (")
                            .append(log.ticketType).append(") - ").append(timeAgo).append("\n");
                }

                statsEmbed.addField("🕒 Recent Activity (Last " + displayCount + ")",
                        recentText.toString().trim(), false);
            }

            // System recommendations
            StringBuilder recommendations = new StringBuilder();
            if (open > 10) {
                recommendations.append("⚠️ High volume of open tickets - consider adding more staff\n");
            }
            if (autoClosed > total * 0.2 && total > 10) {
                recommendations.append("📝 Many auto-closed tickets - review timeout settings\n");
            }
            double reopenRateValue = reopened > 0 ? ((double) reopened / (closed + deleted)) * 100 : 0;
            if (reopenRateValue > 15 && reopened > 3) {
                recommendations.append("🔄 High reopen rate - review closure procedures\n");
            }
            if (recommendations.length() == 0) {
                recommendations.append("✅ System operating normally");
            }

            statsEmbed.addField("💡 Recommendations", recommendations.toString().trim(), false);

            statsEmbed.setFooter("Statistics generated from database • " + guild.getName(),
                    guild.getIconUrl());

            event.getHook().sendMessageEmbeds(statsEmbed.build()).queue();

        } catch (Exception e) {
            System.err.println("❌ Failed to generate statistics: " + e.getMessage());
            e.printStackTrace();
            event.getHook().sendMessage("❌ Failed to generate statistics. Please try again or contact an administrator.")
                    .setEphemeral(true).queue();
        }
    }

    private String getStatusEmoji(String status) {
        return switch (status) {
            case "open" -> "🟢";
            case "closed" -> "🔴";
            case "deleted" -> "🗑️";
            case "auto_closed" -> "⏰";
            case "reopened" -> "🔄";
            default -> "📋";
        };
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "h ago";
        } else if (minutes > 0) {
            return minutes + "m ago";
        } else {
            return "Just now";
        }
    }
}