package com.discordticketbot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TicketLogDAO {
    private final DatabaseManager dbManager;

    public TicketLogDAO() {
        this.dbManager = DatabaseManager.getInstance();
        updateDatabase();
    }

    private void updateDatabase() {
        // Add close_reason column if it doesn't exist
        String addCloseReasonColumn = """
            ALTER TABLE ticket_logs 
            ADD COLUMN IF NOT EXISTS close_reason TEXT
            """;

        try (Connection conn = dbManager.getConnection()) {
            conn.prepareStatement(addCloseReasonColumn).execute();
            System.out.println("✅ Ticket logs table updated with close_reason column");
        } catch (SQLException e) {
            System.err.println("❌ Failed to update ticket logs table: " + e.getMessage());
        }
    }

    /**
     * Log when a ticket is created
     */
    public void logTicketCreated(String guildId, String channelId, String channelName,
                                 String ownerId, String ticketType, int ticketNumber) {
        String query = """
            INSERT INTO ticket_logs (guild_id, channel_id, channel_name, owner_id, ticket_type, ticket_number, status)
            VALUES (?, ?, ?, ?, ?, ?, 'open')
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
            System.out.println("✅ Ticket logged: " + channelName + " (Type: " + ticketType + ")");

        } catch (SQLException e) {
            System.err.println("❌ Failed to log ticket creation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when a ticket is closed
     */
    public void logTicketClosed(String channelId, String closedBy) {
        logTicketClosedWithReason(channelId, closedBy, "No reason provided");
    }

    /**
     * Log when a ticket is closed with a specific reason
     */
    public void logTicketClosedWithReason(String channelId, String closedBy, String closeReason) {
        String query = """
            UPDATE ticket_logs 
            SET closed_at = CURRENT_TIMESTAMP, closed_by = ?, status = 'closed', close_reason = ?
            WHERE channel_id = ? AND status = 'open'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, closedBy);
            stmt.setString(2, closeReason);
            stmt.setString(3, channelId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Ticket closed logged: " + channelId + " by " + closedBy + " - Reason: " + closeReason);
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
            SET status = 'open', closed_at = NULL, closed_by = NULL
            WHERE channel_id = ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            int updated = stmt.executeUpdate();

            if (updated > 0) {
                System.out.println("✅ Ticket reopened logged: " + channelId + " by " + reopenedBy);
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
            SET closed_at = CURRENT_TIMESTAMP, closed_by = ?, status = 'deleted'
            WHERE channel_id = ? AND status != 'deleted'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, deletedBy);
            stmt.setString(2, channelId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Ticket deletion logged: " + channelId + " by " + deletedBy);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to log ticket deletion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when a close request is made
     */
    public void logCloseRequest(String channelId, String requestedBy, String reason, Integer timeoutHours) {
        String query = """
            INSERT INTO ticket_logs (channel_id, action, performed_by, details, created_at)
            VALUES (?, 'close_request', ?, ?, CURRENT_TIMESTAMP)
            """;

        String details = "Reason: " + reason;
        if (timeoutHours != null) {
            details += " | Timeout: " + timeoutHours + " hours";
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.setString(2, requestedBy);
            stmt.setString(3, details);

            stmt.executeUpdate();
            System.out.println("✅ Close request logged: " + channelId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to log close request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when a close request is denied
     */
    public void logCloseRequestDenied(String channelId, String deniedBy) {
        String query = """
            INSERT INTO ticket_logs (channel_id, action, performed_by, details, created_at)
            VALUES (?, 'close_request_denied', ?, 'User denied close request', CURRENT_TIMESTAMP)
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.setString(2, deniedBy);

            stmt.executeUpdate();
            System.out.println("✅ Close request denial logged: " + channelId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to log close request denial: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log when a ticket is auto-closed due to timeout
     */
    public void logTicketAutoClosed(String channelId, int timeoutHours) {
        String query = """
            UPDATE ticket_logs 
            SET closed_at = CURRENT_TIMESTAMP, closed_by = 'SYSTEM', status = 'auto_closed'
            WHERE channel_id = ? AND status = 'open'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.println("✅ Ticket auto-closure logged: " + channelId + " after " + timeoutHours + " hours");
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to log ticket auto-closure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get ticket statistics for a guild
     */
    public TicketStats getGuildTicketStats(String guildId) {
        String query = """
            SELECT 
                COUNT(*) as total_tickets,
                COUNT(CASE WHEN status = 'open' THEN 1 END) as open_tickets,
                COUNT(CASE WHEN status = 'closed' THEN 1 END) as closed_tickets,
                COUNT(CASE WHEN status = 'deleted' THEN 1 END) as deleted_tickets
            FROM ticket_logs 
            WHERE guild_id = ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new TicketStats(
                        rs.getInt("total_tickets"),
                        rs.getInt("open_tickets"),
                        rs.getInt("closed_tickets"),
                        rs.getInt("deleted_tickets")
                );
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get ticket stats: " + e.getMessage());
            e.printStackTrace();
        }

        return new TicketStats(0, 0, 0, 0);
    }

    /**
     * Get recent tickets for a guild
     */
    public List<TicketLogEntry> getRecentTickets(String guildId, int limit) {
        String query = """
            SELECT channel_name, owner_id, ticket_type, ticket_number, created_at, status, close_reason
            FROM ticket_logs 
            WHERE guild_id = ?
            ORDER BY created_at DESC
            LIMIT ?
            """;

        List<TicketLogEntry> tickets = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                tickets.add(new TicketLogEntry(
                        rs.getString("channel_name"),
                        rs.getString("owner_id"),
                        rs.getString("ticket_type"),
                        rs.getInt("ticket_number"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getString("status"),
                        rs.getString("close_reason")
                ));
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get recent tickets: " + e.getMessage());
            e.printStackTrace();
        }

        return tickets;
    }

    // Data classes for ticket information
    public static class TicketStats {
        public final int totalTickets;
        public final int openTickets;
        public final int closedTickets;
        public final int deletedTickets;

        public TicketStats(int total, int open, int closed, int deleted) {
            this.totalTickets = total;
            this.openTickets = open;
            this.closedTickets = closed;
            this.deletedTickets = deleted;
        }
    }

    public static class TicketLogEntry {
        public final String channelName;
        public final String ownerId;
        public final String ticketType;
        public final int ticketNumber;
        public final Instant createdAt;
        public final String status;
        public final String closeReason;

        public TicketLogEntry(String channelName, String ownerId, String ticketType,
                              int ticketNumber, Instant createdAt, String status, String closeReason) {
            this.channelName = channelName;
            this.ownerId = ownerId;
            this.ticketType = ticketType;
            this.ticketNumber = ticketNumber;
            this.createdAt = createdAt;
            this.status = status;
            this.closeReason = closeReason;
        }
    }
}