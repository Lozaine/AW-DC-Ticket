# Discord Ticket Bot

A comprehensive Discord bot for managing support tickets with automated channel creation, permission management, transcript generation, **persistent PostgreSQL database storage**, and advanced ticket management features.

## Features

### 🎫 Ticket Management
- **Three Ticket Types**: Report, Support, and Appeal tickets
- **Automatic Channel Creation**: Creates dedicated channels with proper permissions
- **Global Ticket Counter**: Persistent numbering system stored in database (ticket-username-001, 002, etc.)
- **Permission Management**: Automatically sets up channel permissions for ticket owners and staff
- **Persistent Storage**: All configurations and ticket data stored in PostgreSQL database
- **Ticket Assignment**: Staff can assign tickets to specific team members
- **Auto-Close System**: Configurable timeouts with user confirmation
- **Close Requests**: Staff can request ticket closure with user approval

### 🗄️ Database Features
- **PostgreSQL Integration**: Robust database storage for all configurations
- **Connection Pooling**: HikariCP for efficient database connection management
- **Multi-Guild Support**: Each Discord server has its own configuration stored separately
- **Automatic Migration**: Database tables created automatically on first run
- **Data Persistence**: Ticket counters, configurations, and logs survive bot restarts
- **Ticket Logging**: Comprehensive logging of all ticket activities and statistics
- **Close Request Tracking**: Persistent storage of ticket closure requests and approvals

### 🔧 Administration
- **Easy Setup**: Simple slash command configuration with database persistence
- **Role-Based Access**: Support staff role integration with permission verification
- **Transcript Generation**: Automatic transcript creation with timestamps
- **Channel Management**: Close, reopen, and delete tickets with confirmation
- **Configuration Backup**: All settings automatically saved to database
- **Statistics Dashboard**: Comprehensive ticket analytics and reporting
- **Cleanup Policies**: Configurable automatic cleanup of old logs and requests

### 🛡️ Security & Permissions
- **Administrator Required**: Bot requires Administrator permission for reliable operation
- **Role-Based Permissions**: Support staff and admin role verification
- **Owner Verification**: Ticket ownership validation
- **Secure Database**: Connection pooling with proper credential management
- **Permission Inheritance**: Automatic permission setup for ticket channels

### 📊 Advanced Features
- **Transcript Logging**: Detailed conversation logs with attachments and reactions
- **Timezone Support**: UTC+08:00 (Malaysia/Singapore) timezone formatting
- **Embed Support**: Rich embed messages with proper formatting
- **File Attachment Tracking**: Complete record of uploaded files
- **Guild Isolation**: Each Discord server's data is completely separate
- **Performance Monitoring**: Database connection health checks and diagnostics
- **Error Logging**: Dedicated error logging channel for troubleshooting

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- **PostgreSQL Database** (local or cloud-hosted like Railway, Heroku, etc.)
- Discord Application with Bot Token
- Discord Server with Administrator permissions for the bot

## Installation

### Using Docker (Recommended)

1. **Clone the repository**
   ```bash
   git clone <your-repository-url>
   cd discord-ticket-bot
   ```

2. **Set up environment variables**
   ```bash
   # For development (.env file)
   echo "BOT_TOKEN=your_discord_bot_token_here" > .env
   echo "DATABASE_URL=postgresql://username:password@localhost:5432/ticketbot" >> .env
   
   # For production (environment variables)
   export BOT_TOKEN=your_discord_bot_token_here
   export DATABASE_URL=postgresql://username:password@host:port/database
   ```

3. **Build and run with Docker**
   ```bash
   docker build -t discord-ticket-bot .
   docker run -e BOT_TOKEN=your_discord_bot_token_here \
              -e DATABASE_URL=postgresql://username:password@host:port/database \
              discord-ticket-bot
   ```

### Manual Installation

1. **Clone and build**
   ```bash
   git clone <your-repository-url>
   cd discord-ticket-bot
   mvn clean package
   ```

2. **Set up database**
   ```bash
   # Create PostgreSQL database
   createdb ticketbot
   
   # Set environment variables
   export BOT_TOKEN=your_discord_bot_token_here
   export DATABASE_URL=postgresql://username:password@localhost:5432/ticketbot
   ```

3. **Run the bot**
   ```bash
   java -jar target/AWDCTicket-1.2.0.jar
   ```

## Database Setup

### Local PostgreSQL Setup

1. **Install PostgreSQL**
   ```bash
   # Ubuntu/Debian
   sudo apt update && sudo apt install postgresql postgresql-contrib
   
   # macOS with Homebrew
   brew install postgresql
   
   # Windows: Download from https://www.postgresql.org/download/
   ```

2. **Create database and user**
   ```sql
   sudo -u postgres psql
   CREATE DATABASE ticketbot;
   CREATE USER ticketuser WITH ENCRYPTED PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE ticketbot TO ticketuser;
   \q
   ```

3. **Set DATABASE_URL**
   ```bash
   export DATABASE_URL=postgresql://ticketuser:your_password@localhost:5432/ticketbot
   ```

### Cloud Database (Railway/Heroku)

1. **Railway.app** (Recommended)
   - Create new project on Railway
   - Add PostgreSQL service
   - Copy the provided `DATABASE_URL`
   - Set as environment variable

2. **Heroku Postgres**
   - Add Heroku Postgres add-on to your app
   - Use the provided `DATABASE_URL` from Heroku config vars

3. **Other Cloud Providers**
   - AWS RDS, Google Cloud SQL, DigitalOcean, etc.
   - Format: `postgresql://username:password@host:port/database`

## Discord Bot Setup

1. **Create Discord Application**
   - Go to [Discord Developer Portal](https://discord.com/developers/applications)
   - Create new application and bot
   - Copy the bot token

2. **Bot Permissions**
   - **Administrator** (required for full functionality)
   - Or minimum permissions: Manage Channels, Manage Messages, Send Messages, Embed Links, Attach Files

3. **Invite Bot to Server**
   - Generate invite URL with Administrator permission
   - Add bot to your Discord server

## Configuration

### Initial Setup

1. **Configure the bot** (Administrator only)
   ```
   /setup
   ```
   - Select ticket category (where ticket channels will be created)
   - Choose panel channel (where the ticket creation panel will be posted)
   - Set support roles (staff who can manage tickets)
   - Choose transcript channel (where logs will be saved)
   - **All settings are automatically saved to the database**

2. **Deploy ticket panel**
   ```
   /panel
   ```

### Available Commands

| Command | Description | Permission Required | Database Impact |
|---------|-------------|-------------------|----------------|
| `/help` | Show all available commands | Everyone | None |
| `/setup` | Configure the ticket system | Administrator | Saves configuration to database |
| `/panel` | Send ticket panel to configured channel | Administrator | None |
| `/config` | View current configuration | Administrator | Loads from database |
| `/stats` | View ticket statistics and analytics | Administrator | Queries database for statistics |
| `/cleanup` | Configure automatic cleanup policies | Administrator | Updates cleanup settings |
| `/assign` | Assign ticket to specific staff member | Staff | Updates channel permissions |
| `/closerequest` | Request ticket closure with user approval | Staff | Creates close request in database |
| `/autoclose exclude` | Exclude ticket from auto-close timeouts | Staff | Updates ticket auto-close settings |

## Usage

### For Users
1. Click one of the ticket buttons in the panel channel:
   - 🚨 **Report** - Report users or issues
   - 💬 **Support** - Get help with general questions  
   - ⚖️ **Appeal** - Appeal punishments or decisions

2. Bot creates a private ticket channel with:
   - User access (read, write, attach files)
   - Support staff access (full permissions)
   - Public role denied access
   - **Ticket counter automatically increments in database**

3. **Ticket Management**:
   - Users can request ticket closure
   - Staff can assign tickets to team members
   - Auto-close timeouts with user confirmation

### For Staff
1. **Managing Tickets**:
   - Use 🔒 Close button to close tickets
   - Choose from: Reopen, Generate Transcript, or Delete
   - Assign tickets to specific team members with `/assign`
   - Request ticket closure with `/closerequest`

2. **Transcript Generation**:
   - Automatically saves to configured transcript channel
   - Includes all messages, embeds, attachments, and reactions
   - Formatted with proper timestamps (UTC+08:00)

3. **Administrative Tools**:
   - View comprehensive statistics with `/stats`
   - Configure cleanup policies with `/cleanup`
   - Monitor system health and performance

## Project Structure

```
src/main/java/com/discordticketbot/
├── bot/
│   ├── Main.java              # Application entry point
│   └── TicketBot.java         # Main bot class with database integration
├── config/
│   └── GuildConfig.java       # Server configuration with database persistence
├── database/                  # Database layer
│   ├── DatabaseManager.java   # Database connection management
│   ├── GuildConfigDAO.java    # Data access layer for configurations
│   ├── TicketLogDAO.java      # Ticket logging and statistics
│   └── CloseRequestDAO.java   # Close request management
├── handlers/
│   ├── AssignmentHandler.java # Ticket assignment functionality
│   ├── CleanupHandler.java    # Cleanup policy management
│   ├── CloseRequestHandler.java # Close request processing
│   ├── ConfigHandler.java     # Configuration display
│   ├── HelpHandler.java       # Help command
│   ├── PanelHandler.java      # Panel deployment
│   ├── SetupHandler.java      # Bot setup with database saving
│   ├── StatsHandler.java      # Statistics and analytics
│   └── TicketHandler.java     # Ticket management
├── listeners/
│   ├── ButtonListener.java    # Button interactions
│   ├── CommandListener.java   # Slash commands
│   ├── ModalListener.java     # Modal form handling
│   └── ReadyListener.java     # Bot initialization
└── utils/
    ├── CommandBuilder.java    # Command registration
    ├── CommandDiagnosticUtil.java # Command availability diagnostics
    ├── ErrorLogger.java       # Error logging utilities
    ├── PermissionUtil.java    # Permission checking
    ├── RoleParser.java        # Role parsing utility
    ├── TicketPanelUtil.java   # Panel creation
    ├── TimestampUtil.java     # Timezone and timestamp utilities
    ├── TranscriptUtil.java    # Transcript generation
    └── UserDisplayUtil.java   # User display formatting
```

## Configuration Options

### Environment Variables
- `BOT_TOKEN` - Discord bot token (required)
- `DATABASE_URL` - PostgreSQL connection URL (required)
  - Format: `postgresql://username:password@host:port/database`
  - Example: `postgresql://user:pass@localhost:5432/ticketbot`

### Database Schema
The bot automatically creates these tables:
- **guild_configs** - Server configurations (category, channels, ticket counter)
- **support_roles** - Support staff roles for each server
- **ticket_logs** - Ticket history, statistics, and analytics
- **close_requests** - Ticket closure requests and approvals

### Guild Configuration (per server, stored in database)
- **Category ID** - Where ticket channels are created
- **Panel Channel ID** - Where the ticket panel is displayed  
- **Transcript Channel ID** - Where transcripts are logged
- **Error Log Channel ID** - Where bot errors are logged
- **Support Role IDs** - Staff roles that can manage tickets
- **Ticket Counter** - Persistent numbering for tickets
- **Cleanup Policies** - Automatic cleanup settings for logs and requests

## Features in Detail

### Database Persistence
- **Automatic Backup**: All configurations saved to PostgreSQL
- **Multi-Guild Support**: Each Discord server has isolated data
- **Connection Pooling**: Efficient database connections with HikariCP
- **Auto-Migration**: Database tables created automatically on startup
- **Data Recovery**: Bot can restart without losing any configuration
- **Health Monitoring**: Database connection status and diagnostics

### Ticket Numbering System
- **Database-Backed Counter**: Persistent across bot restarts
- Global counter persists in PostgreSQL database
- Format: `ticket-username-001`, `ticket-username-002`
- Continues numbering even after tickets are deleted
- Automatically detects existing tickets on startup and resumes counting

### Advanced Ticket Management
- **Staff Assignment**: Tickets can be assigned to specific team members
- **Close Requests**: Staff can request closure with user approval
- **Auto-Close System**: Configurable timeouts with user confirmation
- **Permission Inheritance**: Automatic permission setup for ticket channels
- **Ticket Analytics**: Comprehensive statistics and reporting

### Permission Management
- Ticket owner: Read, write, attach files
- Support staff: Full management permissions
- Assigned staff: Explicit channel access
- Public: No access
- Automatic permission updates when tickets are closed/reopened

### Transcript System
- Complete conversation history
- Attachment URLs and file sizes
- Embed content preservation
- Reaction tracking
- UTC+08:00 timezone formatting
- Discord timestamp conversion
- HTML transcript generation (optional)

### Statistics and Analytics
- **Overall Statistics**: Total, open, closed, deleted tickets
- **Ticket Types**: Breakdown by Report, Support, Appeal
- **System Status**: Database connectivity, configuration status
- **Recent Activity**: Latest ticket actions and updates
- **Performance Metrics**: Response times and system health

## Troubleshooting

### Common Issues

1. **Bot not responding to commands**
   - Ensure bot has Administrator permission
   - Check if bot is online and properly invited
   - Verify database connection is working
   - Check command diagnostics with bot logs

2. **Database connection errors**
   - Check `DATABASE_URL` format: `postgresql://username:password@host:port/database`
   - Verify database server is running and accessible
   - Check firewall settings for database port
   - Ensure database user has proper permissions

3. **Cannot create tickets**
   - Verify setup is complete with `/config`
   - Check category and channel permissions
   - Ensure panel channel is under the configured category
   - Check database connectivity

4. **Transcripts not generating**
   - Verify transcript channel exists and bot has access
   - Check file system permissions for transcript folder
   - Ensure database is accessible for configuration retrieval

5. **Configuration not persisting**
   - Check `DATABASE_URL` environment variable
   - Verify database connection in bot logs
   - Ensure PostgreSQL service is running

6. **Statistics not loading**
   - Verify database tables exist and are accessible
   - Check database user permissions
   - Ensure ticket logging is enabled

### Error Messages
- ❌ Administrator permission required
- ❌ Configuration not found - use `/setup`
- ❌ Database connection failed - check DATABASE_URL
- ❌ Channel not found - reconfigure with `/setup`
- ❌ Already have open ticket - check existing channels
- ❌ Staff permission required for this action
- ❌ Database query failed - check connection

### Database Troubleshooting
```bash
# Test database connection
psql $DATABASE_URL -c "SELECT 1;"

# Check if tables exist
psql $DATABASE_URL -c "\dt"

# View guild configurations
psql $DATABASE_URL -c "SELECT * FROM guild_configs;"

# Check ticket logs
psql $DATABASE_URL -c "SELECT COUNT(*) FROM ticket_logs;"

# View close requests
psql $DATABASE_URL -c "SELECT * FROM close_requests LIMIT 5;"
```

## Dependencies

- **JDA (Java Discord API)** 5.0.0-beta.20 - Discord bot framework
- **PostgreSQL Driver** 42.7.1 - Database connectivity
- **HikariCP** 5.1.0 - Database connection pooling
- **dotenv-java** 3.0.0 - Environment variable management
- **SLF4J** 2.0.9 - Logging framework
- **Maven Shade Plugin** - Creates executable JAR with dependencies

## Deployment

### Railway Deployment
1. Connect GitHub repository to Railway
2. Add PostgreSQL service to your Railway project
3. Set environment variables:
   - `BOT_TOKEN=your_discord_bot_token`
   - Railway automatically provides `DATABASE_URL`
4. Deploy and monitor logs

### Heroku Deployment
1. Create Heroku app
2. Add Heroku Postgres add-on
3. Set config vars: `BOT_TOKEN`
4. Deploy via Git or GitHub integration

### Self-Hosted
1. Set up PostgreSQL database
2. Configure environment variables
3. Run the JAR file: `java -jar AWDCTicket-1.2.0.jar`
4. Monitor logs for database connection status

## Contributing

1. Fork the repository
2. Create a feature branch
3. Set up local PostgreSQL database for testing
4. Make your changes
5. Test thoroughly with database operations
6. Submit a pull request

## License

This project is open source. Please check the license file for details.

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review the Discord bot setup requirements
3. Verify database connection and configuration
4. Ensure proper permissions are configured
5. Create an issue with detailed information including:
   - Bot logs
   - Database connection status
   - Environment setup details
   - Command diagnostics output

---

**Note**: This bot requires Administrator permission and a PostgreSQL database to function properly due to the complex permission management, dynamic channel creation, and persistent data storage features required for a robust ticket system.

**Version**: 1.2.0 - Enhanced with advanced ticket management, statistics, assignment system, and comprehensive database logging.
