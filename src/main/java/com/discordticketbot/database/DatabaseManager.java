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
        String databaseUrl = System.getenv("DATABASE_URL");

        // Fallback to .env file for local development
        if (databaseUrl == null || databaseUrl.isBlank()) {
            try {
                Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
                databaseUrl = dotenv.get("DATABASE_URL");
            } catch (Exception e) {
                System.out.println("Note: Could not load DATABASE_URL from .env file");
            }
        }

        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new RuntimeException("DATABASE_URL environment variable is not set. Please configure your PostgreSQL database URL.");
        }

        // Railway provides DATABASE_URL in format: postgresql://username:password@host:port/database
        // We need to convert it to JDBC format: jdbc:postgresql://host:port/database
        String jdbcUrl;
        String username = null;
        String password = null;

        if (databaseUrl.startsWith("postgresql://")) {
            try {
                // Parse Railway DATABASE_URL format
                java.net.URI uri = new java.net.URI(databaseUrl);
                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":");
                    username = userInfo[0];
                    password = userInfo.length > 1 ? userInfo[1] : null;
                }
                jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
                System.out.println("✅ Parsed Railway DATABASE_URL successfully");
            } catch (Exception e) {
                System.err.println("❌ Failed to parse DATABASE_URL: " + e.getMessage());
                throw new RuntimeException("Invalid DATABASE_URL format", e);
            }
        } else if (databaseUrl.startsWith("jdbc:postgresql://")) {
            // Already in JDBC format (local development)
            jdbcUrl = databaseUrl;
            System.out.println("✅ Using JDBC DATABASE_URL format");
        } else {
            throw new RuntimeException("DATABASE_URL must be in PostgreSQL format (postgresql://... or jdbc:postgresql://...)");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (username != null) config.setUsername(username);
        if (password != null) config.setPassword(password);

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
                category_id TEXT,
                panel_channel_id TEXT,
                transcript_channel_id TEXT,
                error_log_channel_id TEXT,
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