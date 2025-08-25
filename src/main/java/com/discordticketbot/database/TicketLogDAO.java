package com.discordticketbot.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketLogDAO {
    private final DatabaseManager dbManager;

    public TicketLogDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Data class to hold ticket log information
     */
    public static class TicketLog {
        public int id;
        public String guildId;
        public String channelId;
        public String channelName;
        public String ownerId;
        public String ticketType;
        public int ticketNumber;
        public Timestamp createdAt;
        public Timestamp closedAt;
        public String closedBy;
        public String status;

        public TicketLog() {}
    }

    /**
     * Log when a ticket is created
     */
    public void logTicketCreated(String guildId, String channelId, String channelName, String ownerId, String ticketType, int ticketNumber) {
        String query = """
            INSERT INTO ticket_logs (guild_id, channel_id, channel_name, owner_id, ticket_type, ticket_number, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, 'open', CURRENT_TIMESTAMP)
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            stmt.setString(2, channelId);
            stmt.setString(3, channelName);
            stmt.setString(4, ownerId);
            stmt.setString(5, ticketType);
            stmt.setInt(6, ticketNumber);

            stmt.executeUpdate();
            System.out.println("✅ Logged ticket creation: " + channelName + " (#" + ticketNumber + ")");

        } catch (SQLException e) {
            System.err.println("❌ Failed to log ticket creation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when a ticket is closed with a reason
     */
    public void logTicketClosedWithReason(String channelId, String closedBy, String reason) {
        String query = """
            UPDATE ticket_logs 
            SET status = 'closed', closed_at = CURRENT_TIMESTAMP, closed_by = ?
            WHERE channel_id = ? AND status = 'open'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, closedBy + " (Reason: " + reason + ")");
            stmt.setString(2, channelId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Logged ticket closure for channel: " + channelId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to log ticket closure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when a ticket is reopened
     */
    public void logTicketReopened(String channelId, String reopenedBy) {
        String query = """
            UPDATE ticket_logs 
            SET status = 'reopened', closed_at = NULL, closed_by = ?
            WHERE channel_id = ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "Reopened by: " + reopenedBy);
            stmt.setString(2, channelId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Logged ticket reopen for channel: " + channelId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to log ticket reopen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when a ticket is deleted
     */
    public void logTicketDeleted(String channelId, String deletedBy) {
        String query = """
            UPDATE ticket_logs 
            SET status = 'deleted', closed_at = CURRENT_TIMESTAMP, closed_by = ?
            WHERE channel_id = ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "Deleted by: " + deletedBy);
            stmt.setString(2, channelId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Logged ticket deletion for channel: " + channelId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to log ticket deletion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log close request
     */
    public void logCloseRequest(String channelId, String requestedBy, String reason, Integer timeoutHours) {
        String query = """
            INSERT INTO ticket_logs (guild_id, channel_id, channel_name, owner_id, ticket_type, ticket_number, status, created_at, closed_by)
            SELECT guild_id, channel_id, channel_name || ' - Close Request', owner_id, 'close_request', 0, 'close_requested', CURRENT_TIMESTAMP, ?
            FROM ticket_logs 
            WHERE channel_id = ? AND status != 'close_requested'
            LIMIT 1
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String logEntry = "Close requested by: " + requestedBy + " | Reason: " + reason;
            if (timeoutHours != null) {
                logEntry += " | Timeout: " + timeoutHours + "h";
            }

            stmt.setString(1, logEntry);
            stmt.setString(2, channelId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to log close request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when close request is denied
     */
    public void logCloseRequestDenied(String channelId, String deniedBy) {
        String query = """
            INSERT INTO ticket_logs (guild_id, channel_id, channel_name, owner_id, ticket_type, ticket_number, status, created_at, closed_by)
            SELECT guild_id, channel_id, channel_name || ' - Close Denied', owner_id, 'close_denied', 0, 'close_denied', CURRENT_TIMESTAMP, ?
            FROM ticket_logs 
            WHERE channel_id = ? AND status = 'open'
            LIMIT 1
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "Close request denied by: " + deniedBy);
            stmt.setString(2, channelId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to log close request denial: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when ticket is auto-closed
     */
    public void logTicketAutoClosed(String channelId, int timeoutHours) {
        String query = """
            UPDATE ticket_logs 
            SET status = 'auto_closed', closed_at = CURRENT_TIMESTAMP, closed_by = ?
            WHERE channel_id = ? AND status = 'open'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "Auto-closed after " + timeoutHours + " hours timeout");
            stmt.setString(2, channelId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Logged ticket auto-closure for channel: " + channelId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to log ticket auto-closure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get ticket statistics for a guild
     */
    public Map<String, Integer> getTicketStats(String guildId) {
        Map<String, Integer> stats = new HashMap<>();

        String query = """
            SELECT 
                COUNT(*) as total_tickets,
                COUNT(CASE WHEN status = 'open' THEN 1 END) as open_tickets,
                COUNT(CASE WHEN status = 'closed' THEN 1 END) as closed_tickets,
                COUNT(CASE WHEN status = 'deleted' THEN 1 END) as deleted_tickets,
                COUNT(CASE WHEN status = 'auto_closed' THEN 1 END) as auto_closed_tickets,
                COUNT(CASE WHEN status = 'reopened' THEN 1 END) as reopened_tickets
            FROM ticket_logs 
            WHERE guild_id = ? AND ticket_type != 'close_request' AND ticket_type != 'close_denied'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                stats.put("total", rs.getInt("total_tickets"));
                stats.put("open", rs.getInt("open_tickets"));
                stats.put("closed", rs.getInt("closed_tickets"));
                stats.put("deleted", rs.getInt("deleted_tickets"));
                stats.put("auto_closed", rs.getInt("auto_closed_tickets"));
                stats.put("reopened", rs.getInt("reopened_tickets"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get ticket statistics: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Get ticket statistics by type for a guild
     */
    public Map<String, Integer> getTicketStatsByType(String guildId) {
        Map<String, Integer> stats = new HashMap<>();

        String query = """
            SELECT ticket_type, COUNT(*) as count
            FROM ticket_logs 
            WHERE guild_id = ? AND ticket_type != 'close_request' AND ticket_type != 'close_denied'
            GROUP BY ticket_type
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                stats.put(rs.getString("ticket_type"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get ticket statistics by type: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Get recent ticket activity for a guild
     */
    public List<TicketLog> getRecentTicketActivity(String guildId, int limit) {
        List<TicketLog> logs = new ArrayList<>();

        String query = """
            SELECT id, guild_id, channel_id, channel_name, owner_id, ticket_type, ticket_number,
                   created_at, closed_at, closed_by, status
            FROM ticket_logs 
            WHERE guild_id = ?
            ORDER BY created_at DESC 
            LIMIT ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                TicketLog log = new TicketLog();
                log.id = rs.getInt("id");
                log.guildId = rs.getString("guild_id");
                log.channelId = rs.getString("channel_id");
                log.channelName = rs.getString("channel_name");
                log.ownerId = rs.getString("owner_id");
                log.ticketType = rs.getString("ticket_type");
                log.ticketNumber = rs.getInt("ticket_number");
                log.createdAt = rs.getTimestamp("created_at");
                log.closedAt = rs.getTimestamp("closed_at");
                log.closedBy = rs.getString("closed_by");
                log.status = rs.getString("status");
                logs.add(log);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get recent ticket activity: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }

    /**
     * Get tickets by user for a guild
     */
    public List<TicketLog> getTicketsByUser(String guildId, String userId) {
        List<TicketLog> logs = new ArrayList<>();

        String query = """
            SELECT id, guild_id, channel_id, channel_name, owner_id, ticket_type, ticket_number,
                   created_at, closed_at, closed_by, status
            FROM ticket_logs 
            WHERE guild_id = ? AND owner_id = ? AND ticket_type != 'close_request' AND ticket_type != 'close_denied'
            ORDER BY created_at DESC
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            stmt.setString(2, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                TicketLog log = new TicketLog();
                log.id = rs.getInt("id");
                log.guildId = rs.getString("guild_id");
                log.channelId = rs.getString("channel_id");
                log.channelName = rs.getString("channel_name");
                log.ownerId = rs.getString("owner_id");
                log.ticketType = rs.getString("ticket_type");
                log.ticketNumber = rs.getInt("ticket_number");
                log.createdAt = rs.getTimestamp("created_at");
                log.closedAt = rs.getTimestamp("closed_at");
                log.closedBy = rs.getString("closed_by");
                log.status = rs.getString("status");
                logs.add(log);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get tickets by user: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }

    /**
     * Clean up old ticket logs (optional maintenance method)
     */
    public void cleanupOldTicketLogs(int daysOld) {
        String query = """
            DELETE FROM ticket_logs 
            WHERE status IN ('deleted', 'closed') AND closed_at < CURRENT_TIMESTAMP - INTERVAL '? days'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, daysOld);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("🧹 Cleaned up " + deleted + " old ticket logs");
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to cleanup old ticket logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get the highest ticket number for counter initialization
     */
    public int getHighestTicketNumber(String guildId) {
        String query = """
            SELECT COALESCE(MAX(ticket_number), 0) as highest_number
            FROM ticket_logs 
            WHERE guild_id = ? AND ticket_type != 'close_request' AND ticket_type != 'close_denied'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("highest_number");
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get highest ticket number: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }
}