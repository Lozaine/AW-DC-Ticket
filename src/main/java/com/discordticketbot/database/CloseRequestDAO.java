package com.discordticketbot.database;

import java.sql.*;
import java.time.LocalDateTime;

public class CloseRequestDAO {
    private final DatabaseManager dbManager;

    public CloseRequestDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Data class to hold close request details
     */
    public static class CloseRequestDetails {
        public String channelId;
        public String requestedBy;
        public String ticketOwner;
        public String reason;
        public Integer timeoutHours;
        public String status;
        public Timestamp createdAt;
        public Timestamp respondedAt;
        public String respondedBy;
        public boolean excludedFromAutoClose;
        public String messageId;

        public CloseRequestDetails() {}
    }

    /**
     * Create a new close request
     */
    public void createCloseRequest(String channelId, String requestedBy, String ticketOwner, String reason, Integer timeoutHours) {
        String query = """
            INSERT INTO close_requests (channel_id, requested_by, ticket_owner, reason, timeout_hours, status, created_at)
            VALUES (?, ?, ?, ?, ?, 'pending', CURRENT_TIMESTAMP)
            ON CONFLICT (channel_id) 
            DO UPDATE SET 
                requested_by = EXCLUDED.requested_by,
                reason = EXCLUDED.reason,
                timeout_hours = EXCLUDED.timeout_hours,
                status = 'pending',
                created_at = CURRENT_TIMESTAMP,
                responded_at = NULL,
                responded_by = NULL
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.setString(2, requestedBy);
            stmt.setString(3, ticketOwner);
            stmt.setString(4, reason);
            if (timeoutHours != null) {
                stmt.setInt(5, timeoutHours);
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to create close request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if there's an active close request for a channel
     */
    public boolean hasActiveCloseRequest(String channelId) {
        String query = "SELECT COUNT(*) FROM close_requests WHERE channel_id = ? AND status = 'pending'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to check active close requests: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get close request details
     */
    public CloseRequestDetails getCloseRequestDetails(String channelId) {
        String query = """
            SELECT channel_id, requested_by, ticket_owner, reason, timeout_hours, status, 
                   created_at, responded_at, responded_by, excluded_from_autoclose, message_id
            FROM close_requests 
            WHERE channel_id = ? AND status = 'pending'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                CloseRequestDetails details = new CloseRequestDetails();
                details.channelId = rs.getString("channel_id");
                details.requestedBy = rs.getString("requested_by");
                details.ticketOwner = rs.getString("ticket_owner");
                details.reason = rs.getString("reason");
                details.timeoutHours = rs.getObject("timeout_hours", Integer.class);
                details.status = rs.getString("status");
                details.createdAt = rs.getTimestamp("created_at");
                details.respondedAt = rs.getTimestamp("responded_at");
                details.respondedBy = rs.getString("responded_by");
                details.excludedFromAutoClose = rs.getBoolean("excluded_from_autoclose");
                details.messageId = rs.getString("message_id");
                return details;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to get close request details: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Update close request with message ID
     */
    public void updateCloseRequestMessageId(String channelId, String messageId) {
        String query = "UPDATE close_requests SET message_id = ? WHERE channel_id = ? AND status = 'pending'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, messageId);
            stmt.setString(2, channelId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to update close request message ID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Confirm close request
     */
    public void confirmCloseRequest(String channelId, String respondedBy) {
        String query = """
            UPDATE close_requests 
            SET status = 'confirmed', responded_at = CURRENT_TIMESTAMP, responded_by = ?
            WHERE channel_id = ? AND status = 'pending'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, respondedBy);
            stmt.setString(2, channelId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to confirm close request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deny close request
     */
    public void denyCloseRequest(String channelId, String respondedBy) {
        String query = """
            UPDATE close_requests 
            SET status = 'denied', responded_at = CURRENT_TIMESTAMP, responded_by = ?
            WHERE channel_id = ? AND status = 'pending'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, respondedBy);
            stmt.setString(2, channelId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to deny close request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Auto-close request (timeout)
     */
    public void autoCloseRequest(String channelId) {
        String query = """
            UPDATE close_requests 
            SET status = 'auto_closed', responded_at = CURRENT_TIMESTAMP, responded_by = 'SYSTEM'
            WHERE channel_id = ? AND status = 'pending'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to auto-close request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Exclude ticket from auto-close
     */
    public void excludeFromAutoClose(String channelId, String excludedBy) {
        String query = """
            INSERT INTO close_requests (channel_id, requested_by, ticket_owner, reason, status, excluded_from_autoclose, created_at)
            VALUES (?, ?, 'SYSTEM', 'Excluded from auto-close', 'excluded', TRUE, CURRENT_TIMESTAMP)
            ON CONFLICT (channel_id)
            DO UPDATE SET 
                excluded_from_autoclose = TRUE,
                status = 'excluded'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.setString(2, excludedBy);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to exclude from auto-close: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if ticket is excluded from auto-close
     */
    public boolean isExcludedFromAutoClose(String channelId) {
        String query = "SELECT excluded_from_autoclose FROM close_requests WHERE channel_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("excluded_from_autoclose");
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to check auto-close exclusion: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Clean up old close requests (optional maintenance method)
     */
    public void cleanupOldCloseRequests(int daysOld) {
        String query = """
            DELETE FROM close_requests 
            WHERE status != 'pending' AND created_at < CURRENT_TIMESTAMP - INTERVAL '? days'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, daysOld);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("🧹 Cleaned up " + deleted + " old close requests");
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to cleanup old close requests: " + e.getMessage());
            e.printStackTrace();
        }
    }
}