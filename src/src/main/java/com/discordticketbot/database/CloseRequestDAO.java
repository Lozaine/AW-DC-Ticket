
package com.discordticketbot.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CloseRequestDAO {
    private final DatabaseManager dbManager;

    public CloseRequestDAO() {
        this.dbManager = new DatabaseManager();
        createTables();
    }

    private void createTables() {
        String createCloseRequestsTable = """
            CREATE TABLE IF NOT EXISTS close_requests (
                id SERIAL PRIMARY KEY,
                channel_id VARCHAR(20) NOT NULL,
                requested_by VARCHAR(20) NOT NULL,
                ticket_owner VARCHAR(20) NOT NULL,
                reason TEXT,
                timeout_hours INTEGER,
                message_id VARCHAR(20),
                status VARCHAR(20) DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                responded_at TIMESTAMP,
                UNIQUE(channel_id, status) DEFERRABLE INITIALLY DEFERRED
            )
            """;

        String createAutoCloseExclusionsTable = """
            CREATE TABLE IF NOT EXISTS autoclose_exclusions (
                id SERIAL PRIMARY KEY,
                channel_id VARCHAR(20) UNIQUE NOT NULL,
                excluded_by VARCHAR(20) NOT NULL,
                excluded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection conn = dbManager.getConnection()) {
            conn.prepareStatement(createCloseRequestsTable).execute();
            conn.prepareStatement(createAutoCloseExclusionsTable).execute();
            System.out.println("✅ Close request tables created/verified");
        } catch (SQLException e) {
            System.err.println("❌ Failed to create close request tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createCloseRequest(String channelId, String requestedBy, String ticketOwner, String reason, Integer timeoutHours) {
        String query = """
            INSERT INTO close_requests (channel_id, requested_by, ticket_owner, reason, timeout_hours, status)
            VALUES (?, ?, ?, ?, ?, 'pending')
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
                stmt.setNull(5, java.sql.Types.INTEGER);
            }

            stmt.executeUpdate();
            System.out.println("✅ Close request created: " + channelId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to create close request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean hasActiveCloseRequest(String channelId) {
        String query = "SELECT 1 FROM close_requests WHERE channel_id = ? AND status = 'pending'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ Failed to check active close request: " + e.getMessage());
            return false;
        }
    }

    public void updateCloseRequestMessageId(String channelId, String messageId) {
        String query = """
            UPDATE close_requests 
            SET message_id = ? 
            WHERE channel_id = ? AND status = 'pending'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, messageId);
            stmt.setString(2, channelId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to update close request message ID: " + e.getMessage());
        }
    }

    public void confirmCloseRequest(String channelId, String userId) {
        String query = """
            UPDATE close_requests 
            SET status = 'confirmed', responded_at = CURRENT_TIMESTAMP
            WHERE channel_id = ? AND ticket_owner = ? AND status = 'pending'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.setString(2, userId);
            stmt.executeUpdate();
            System.out.println("✅ Close request confirmed: " + channelId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to confirm close request: " + e.getMessage());
        }
    }

    public void denyCloseRequest(String channelId, String userId) {
        String query = """
            UPDATE close_requests 
            SET status = 'denied', responded_at = CURRENT_TIMESTAMP
            WHERE channel_id = ? AND ticket_owner = ? AND status = 'pending'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.setString(2, userId);
            stmt.executeUpdate();
            System.out.println("✅ Close request denied: " + channelId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to deny close request: " + e.getMessage());
        }
    }

    public void autoCloseRequest(String channelId) {
        String query = """
            UPDATE close_requests 
            SET status = 'auto_closed', responded_at = CURRENT_TIMESTAMP
            WHERE channel_id = ? AND status = 'pending'
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.executeUpdate();
            System.out.println("✅ Close request auto-closed: " + channelId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to auto-close request: " + e.getMessage());
        }
    }

    public void excludeFromAutoClose(String channelId, String excludedBy) {
        String query = """
            INSERT INTO autoclose_exclusions (channel_id, excluded_by)
            VALUES (?, ?)
            ON CONFLICT (channel_id) DO NOTHING
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            stmt.setString(2, excludedBy);
            stmt.executeUpdate();
            System.out.println("✅ Channel excluded from auto-close: " + channelId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to exclude from auto-close: " + e.getMessage());
        }
    }

    public boolean isExcludedFromAutoClose(String channelId) {
        String query = "SELECT 1 FROM autoclose_exclusions WHERE channel_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ Failed to check auto-close exclusion: " + e.getMessage());
            return false;
        }
    }
}
