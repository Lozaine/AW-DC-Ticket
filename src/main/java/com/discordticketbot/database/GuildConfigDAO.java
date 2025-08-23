package com.discordticketbot.database;

import com.discordticketbot.config.GuildConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildConfigDAO {
    private final DatabaseManager dbManager;

    public GuildConfigDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Save or update guild configuration
     */
    public void saveGuildConfig(String guildId, GuildConfig config) {
        String upsertQuery = """
            INSERT INTO guild_configs (guild_id, category_id, panel_channel_id, transcript_channel_id, ticket_counter, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (guild_id) 
            DO UPDATE SET 
                category_id = EXCLUDED.category_id,
                panel_channel_id = EXCLUDED.panel_channel_id,
                transcript_channel_id = EXCLUDED.transcript_channel_id,
                ticket_counter = EXCLUDED.ticket_counter,
                updated_at = CURRENT_TIMESTAMP
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(upsertQuery)) {

            stmt.setString(1, guildId);
            stmt.setString(2, config.categoryId);
            stmt.setString(3, config.panelChannelId);
            stmt.setString(4, config.transcriptChannelId);
            stmt.setInt(5, config.ticketCounter);

            stmt.executeUpdate();

            // Save support roles
            saveSupportRoles(guildId, config.supportRoleIds);

            System.out.println("✅ Guild config saved for: " + guildId);

        } catch (SQLException e) {
            System.err.println("❌ Failed to save guild config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load guild configuration from database
     */
    public GuildConfig loadGuildConfig(String guildId) {
        String query = """
            SELECT category_id, panel_channel_id, transcript_channel_id, ticket_counter
            FROM guild_configs 
            WHERE guild_id = ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                GuildConfig config = new GuildConfig();
                config.categoryId = rs.getString("category_id");
                config.panelChannelId = rs.getString("panel_channel_id");
                config.transcriptChannelId = rs.getString("transcript_channel_id");
                config.ticketCounter = rs.getInt("ticket_counter");

                // Load support roles
                config.supportRoleIds = loadSupportRoles(guildId);

                return config;
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to load guild config: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Load all guild configurations
     */
    public Map<String, GuildConfig> loadAllGuildConfigs() {
        Map<String, GuildConfig> configs = new HashMap<>();
        String query = """
            SELECT guild_id, category_id, panel_channel_id, transcript_channel_id, ticket_counter
            FROM guild_configs
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String guildId = rs.getString("guild_id");
                GuildConfig config = new GuildConfig();
                config.categoryId = rs.getString("category_id");
                config.panelChannelId = rs.getString("panel_channel_id");
                config.transcriptChannelId = rs.getString("transcript_channel_id");
                config.ticketCounter = rs.getInt("ticket_counter");

                // Load support roles for this guild
                config.supportRoleIds = loadSupportRoles(guildId);

                configs.put(guildId, config);
            }

            System.out.println("✅ Loaded " + configs.size() + " guild configurations from database");

        } catch (SQLException e) {
            System.err.println("❌ Failed to load guild configs: " + e.getMessage());
            e.printStackTrace();
        }

        return configs;
    }

    /**
     * Update ticket counter for a guild
     */
    public void updateTicketCounter(String guildId, int newCounter) {
        String query = """
            UPDATE guild_configs 
            SET ticket_counter = ?, updated_at = CURRENT_TIMESTAMP 
            WHERE guild_id = ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, newCounter);
            stmt.setString(2, guildId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Failed to update ticket counter: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save support roles for a guild
     */
    private void saveSupportRoles(String guildId, java.util.Set<String> roleIds) {
        // Delete existing roles
        String deleteQuery = "DELETE FROM support_roles WHERE guild_id = ?";

        // Insert new roles
        String insertQuery = "INSERT INTO support_roles (guild_id, role_id) VALUES (?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            // Delete existing
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                deleteStmt.setString(1, guildId);
                deleteStmt.executeUpdate();
            }

            // Insert new roles
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                for (String roleId : roleIds) {
                    insertStmt.setString(1, guildId);
                    insertStmt.setString(2, roleId);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to save support roles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load support roles for a guild
     */
    private java.util.Set<String> loadSupportRoles(String guildId) {
        java.util.Set<String> roleIds = new java.util.HashSet<>();
        String query = "SELECT role_id FROM support_roles WHERE guild_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                roleIds.add(rs.getString("role_id"));
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to load support roles: " + e.getMessage());
            e.printStackTrace();
        }

        return roleIds;
    }

    /**
     * Delete guild configuration
     */
    public void deleteGuildConfig(String guildId) {
        String query = "DELETE FROM guild_configs WHERE guild_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, guildId);
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                System.out.println("✅ Guild config deleted for: " + guildId);
            }

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete guild config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}