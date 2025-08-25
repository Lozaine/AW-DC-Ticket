package com.discordticketbot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    private DatabaseManager() {
        initializeDatabase();
        createTables();
        runMigrations(); // Add migration step
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
        // Create the base tables first
        String createGuildConfigTable = """
            CREATE TABLE IF NOT EXISTS guild_configs (
                guild_id VARCHAR(20) PRIMARY KEY,
                category_id TEXT,
                panel_channel_id TEXT,
                transcript_channel_id TEXT,
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
                closed_by VARCHAR(255),
                status VARCHAR(20) DEFAULT 'open'
            )
            """;

        // Add close requests table for the CloseRequestHandler
        String createCloseRequestsTable = """
            CREATE TABLE IF NOT EXISTS close_requests (
                id SERIAL PRIMARY KEY,
                channel_id VARCHAR(20) NOT NULL UNIQUE,
                requested_by VARCHAR(20) NOT NULL,
                ticket_owner VARCHAR(20) NOT NULL,
                reason TEXT,
                timeout_hours INTEGER,
                message_id VARCHAR(20),
                status VARCHAR(20) DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                responded_at TIMESTAMP,
                responded_by VARCHAR(20),
                excluded_from_autoclose BOOLEAN DEFAULT FALSE
            )
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Execute each table creation separately with error handling
            try {
                stmt.execute(createGuildConfigTable);
                System.out.println("✅ guild_configs table created/verified");
            } catch (SQLException e) {
                System.err.println("❌ Failed to create guild_configs table: " + e.getMessage());
                throw e;
            }

            try {
                stmt.execute(createSupportRolesTable);
                System.out.println("✅ support_roles table created/verified");
            } catch (SQLException e) {
                System.err.println("❌ Failed to create support_roles table: " + e.getMessage());
                throw e;
            }

            try {
                stmt.execute(createTicketLogsTable);
                System.out.println("✅ ticket_logs table created/verified");
            } catch (SQLException e) {
                System.err.println("❌ Failed to create ticket_logs table: " + e.getMessage());
                throw e;
            }

            try {
                stmt.execute(createCloseRequestsTable);
                System.out.println("✅ close_requests table created/verified");
            } catch (SQLException e) {
                System.err.println("❌ Failed to create close_requests table: " + e.getMessage());
                throw e;
            }

            System.out.println("✅ Database tables initialized successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Run database migrations to add new columns and update schema
     */
    private void runMigrations() {
        try (Connection conn = getConnection()) {
            System.out.println("🔄 Running database migrations...");

            // Migration 1: Add error_log_channel_id column to guild_configs
            migrateGuildConfigsTable(conn);

            // Migration 2: Fix ticket_logs table schema
            migrateTicketLogsTable(conn);

            // Migration 3: Fix close_requests table schema
            migrateCloseRequestsTable(conn);

            System.out.println("✅ Database migrations completed successfully!");

        } catch (SQLException e) {
            System.err.println("❌ Failed to run database migrations: " + e.getMessage());
            e.printStackTrace();
            // Don't throw here - let the bot continue with existing schema
        }
    }

    /**
     * Migrate guild_configs table to add error_log_channel_id column if it doesn't exist
     */
    private void migrateGuildConfigsTable(Connection conn) throws SQLException {
        // Check if error_log_channel_id column exists
        String checkColumnQuery = """
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_name = 'guild_configs' 
            AND column_name = 'error_log_channel_id'
            """;

        try (PreparedStatement stmt = conn.prepareStatement(checkColumnQuery)) {
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                // Column doesn't exist, add it
                System.out.println("🔄 Migrating guild_configs table: adding error_log_channel_id column...");

                String addColumnQuery = """
                    ALTER TABLE guild_configs 
                    ADD COLUMN error_log_channel_id VARCHAR(20)
                    """;

                try (Statement alterStmt = conn.createStatement()) {
                    alterStmt.execute(addColumnQuery);
                    System.out.println("✅ Successfully added error_log_channel_id column to guild_configs table");
                }
            } else {
                System.out.println("✅ error_log_channel_id column already exists in guild_configs table");
            }
        }
    }

    /**
     * Migrate ticket_logs table to fix schema issues
     */
    private void migrateTicketLogsTable(Connection conn) throws SQLException {
        // Check if ticket_number column exists
        String checkColumnQuery = """
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_name = 'ticket_logs' 
            AND column_name = 'ticket_number'
            """;

        try (PreparedStatement stmt = conn.prepareStatement(checkColumnQuery)) {
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                // Column doesn't exist, add it
                System.out.println("🔄 Migrating ticket_logs table: adding ticket_number column...");

                String addColumnQuery = """
                    ALTER TABLE ticket_logs 
                    ADD COLUMN ticket_number INTEGER
                    """;

                try (Statement alterStmt = conn.createStatement()) {
                    alterStmt.execute(addColumnQuery);
                    System.out.println("✅ Successfully added ticket_number column to ticket_logs table");
                }
            } else {
                System.out.println("✅ ticket_number column already exists in ticket_logs table");
            }
        }

        // Check and alter closed_by column length if needed
        String checkClosedByQuery = """
            SELECT character_maximum_length 
            FROM information_schema.columns 
            WHERE table_name = 'ticket_logs' 
            AND column_name = 'closed_by'
            """;

        try (PreparedStatement stmt = conn.prepareStatement(checkClosedByQuery)) {
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Integer maxLength = rs.getObject("character_maximum_length", Integer.class);
                if (maxLength != null && maxLength < 255) {
                    System.out.println("🔄 Migrating ticket_logs table: altering closed_by column length from " + maxLength + " to 255...");

                    String alterColumnQuery = """
                        ALTER TABLE ticket_logs 
                        ALTER COLUMN closed_by TYPE VARCHAR(255)
                        """;

                    try (Statement alterStmt = conn.createStatement()) {
                        alterStmt.execute(alterColumnQuery);
                        System.out.println("✅ Successfully altered closed_by column length to 255");
                    }
                } else {
                    System.out.println("✅ closed_by column already has sufficient length");
                }
            }
        }
    }

    /**
     * Migrate close_requests table to fix schema issues
     */
    private void migrateCloseRequestsTable(Connection conn) throws SQLException {
        // Check if excluded_from_autoclose column exists
        String checkColumnQuery = """
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_name = 'close_requests' 
            AND column_name = 'excluded_from_autoclose'
            """;

        try (PreparedStatement stmt = conn.prepareStatement(checkColumnQuery)) {
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                // Column doesn't exist, add it
                System.out.println("🔄 Migrating close_requests table: adding excluded_from_autoclose column...");

                String addColumnQuery = """
                    ALTER TABLE close_requests 
                    ADD COLUMN excluded_from_autoclose BOOLEAN DEFAULT FALSE
                    """;

                try (Statement alterStmt = conn.createStatement()) {
                    alterStmt.execute(addColumnQuery);
                    System.out.println("✅ Successfully added excluded_from_autoclose column to close_requests table");
                }
            } else {
                System.out.println("✅ excluded_from_autoclose column already exists in close_requests table");
            }
        }

        // Check if responded_by column exists
        String checkRespondedByQuery = """
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_name = 'close_requests' 
            AND column_name = 'responded_by'
            """;

        try (PreparedStatement stmt = conn.prepareStatement(checkRespondedByQuery)) {
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                // Column doesn't exist, add it
                System.out.println("🔄 Migrating close_requests table: adding responded_by column...");

                String addColumnQuery = """
                    ALTER TABLE close_requests 
                    ADD COLUMN responded_by VARCHAR(20)
                    """;

                try (Statement alterStmt = conn.createStatement()) {
                    alterStmt.execute(addColumnQuery);
                    System.out.println("✅ Successfully added responded_by column to close_requests table");
                }
            } else {
                System.out.println("✅ responded_by column already exists in close_requests table");
            }
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

    /**
     * Test database connectivity and schema
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            // Test basic connectivity
            conn.isValid(5);

            // Test if our tables exist
            String testQuery = "SELECT COUNT(*) FROM guild_configs LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(testQuery)) {
                stmt.executeQuery();
            }

            System.out.println("✅ Database connection and schema test successful!");
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}