package com.discordticketbot.web.service;

import com.discordticketbot.database.DatabaseManager;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GuildConfigService {
    
    private final DatabaseManager databaseManager;
    
    public GuildConfigService() {
        this.databaseManager = DatabaseManager.getInstance();
    }
    
    public Map<String, Object> getGuildConfig(String guildId) {
        Map<String, Object> config = new HashMap<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM guild_configs WHERE guild_id = ?"
             )) {
            
            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                config.put("guildId", rs.getString("guild_id"));
                config.put("categoryId", rs.getString("category_id"));
                config.put("panelChannelId", rs.getString("panel_channel_id"));
                config.put("transcriptChannelId", rs.getString("transcript_channel_id"));
                config.put("ticketCounter", rs.getInt("ticket_counter"));
                config.put("errorLogChannelId", rs.getString("error_log_channel_id"));
            }
            
            // Get support roles
            List<String> supportRoles = getSupportRoles(guildId, conn);
            config.put("supportRoles", supportRoles);
            
        } catch (SQLException e) {
            System.err.println("Error fetching guild config: " + e.getMessage());
            e.printStackTrace();
        }
        
        return config;
    }
    
    private List<String> getSupportRoles(String guildId, Connection conn) throws SQLException {
        List<String> roles = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT role_id FROM support_roles WHERE guild_id = ?"
            )) {
            
            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                roles.add(rs.getString("role_id"));
            }
        }
        
        return roles;
    }
    
    public List<Map<String, Object>> getTicketLogs(String guildId) {
        List<Map<String, Object>> tickets = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM ticket_logs WHERE guild_id = ? ORDER BY created_at DESC"
             )) {
            
            stmt.setString(1, guildId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> ticket = new HashMap<>();
                ticket.put("id", rs.getInt("id"));
                ticket.put("channelId", rs.getString("channel_id"));
                ticket.put("channelName", rs.getString("channel_name"));
                ticket.put("ownerId", rs.getString("owner_id"));
                ticket.put("ticketType", rs.getString("ticket_type"));
                ticket.put("ticketNumber", rs.getInt("ticket_number"));
                ticket.put("createdAt", rs.getTimestamp("created_at"));
                ticket.put("closedAt", rs.getTimestamp("closed_at"));
                ticket.put("closedBy", rs.getString("closed_by"));
                ticket.put("status", rs.getString("status"));
                
                tickets.add(ticket);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching ticket logs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tickets;
    }
}