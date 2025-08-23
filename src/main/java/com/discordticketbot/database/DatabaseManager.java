package com.discordticketbot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {
        initializeDatabase();
        createTables();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        String databaseUrl;
        String username;
        String password;

        // Try environment variables first (Railway production)
        databaseUrl = System.getenv("DATABASE_URL");
        username = System.getenv("DATABASE_USERNAME");
        password = System.getenv("DATABASE_PASSWORD");

        // Fallback to .env file for development
        if (databaseUrl == null || databaseUrl.isBlank()) {
            try {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                databaseUrl = dotenv.get("DATABASE_URL");
                username = dotenv.get("DATABASE_USERNAME");
                password = dotenv.get("DATABASE_PASSWORD");
            } catch (Exception e) {
                System.out.println("Note: Could not load database config from .env file");
            }
        }

        // Railway typically provides DATABASE_URL in the format:
        // postgresql://username:password@host:port/database
        if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
            // Parse Railway DATABASE_URL format
            try {
                java.net.URI uri = new java.net.URI(databaseUrl.substring(13)); // Remove "postgresql://"
                String[] userInfo = uri.getUserInfo().split(":");
                username = userInfo[0];
                password = userInfo[1];
                databaseUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
            } catch (Exception e) {
                System.err.println("Failed to parse DATABASE_URL: " + e.getMessage());
            }
        }

        if (databaseUrl == null || username == null || password == null) {
            throw new RuntimeException("Database configuration not found. Please set DATABASE_URL, DATABASE_USERNAME, and DATABASE_PASSWORD environment variables.");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseUrl);
        config.setUsername(username);
        config.setPassword(password);

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // PostgreSQL specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            this.dataSource = new HikariDataSource(config);
            System.out.println("✅ Database connection established successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to establish database connection: " + e.getMessage());
            throw new RuntimeException("Database connection failed", e);
        }
    }

    private void createTables() {
        String createGuildConfigTable = """
            CREATE TABLE IF NOT EXISTS guild_configs (
                guild_id VARCHAR(20) PRIMARY KEY,
                category_id VARCHAR(20),
                panel_channel_id VARCHAR(20),
                transcript_channel_id VARCHAR(20),
                ticket_counter INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createSupportRolesTable = """
            CREATE TABLE IF NOT EXISTS support_roles (
                id SERIAL PRIMARY KEY,
                guild_id VARCHAR(20) NOT NULL,
                role_id VARCHAR(20) NOT NULL,
                FOREIGN KEY (guild_id) REFERENCES guild_configs(guild_id) ON DELETE CASCADE,
                UNIQUE(guild_id, role_id)
            )
            """;

        String createTicketLogsTable = """
            CREATE TABLE IF NOT EXISTS ticket_logs (
                id SERIAL PRIMARY KEY,
                guild_id VARCHAR(20) NOT NULL,
                channel_id VARCHAR(20),
                channel_name VARCHAR(100),
                owner_id VARCHAR(20),
                ticket_type VARCHAR(20),
                ticket_number INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                closed_at TIMESTAMP,
                closed_by VARCHAR(20),
                status VARCHAR(20) DEFAULT 'open'
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createGuildConfigTable);
            stmt.execute(createSupportRolesTable);
            stmt.execute(createTicketLogsTable);

            System.out.println("✅ Database tables initialized successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Database connection pool closed.");
        }
    }
}