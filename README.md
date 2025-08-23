# Discord Ticket Bot

A comprehensive Discord bot for managing support tickets with automated channel creation, permission management, and transcript generation.

## Features

### 🎫 Ticket Management
- **Three Ticket Types**: Report, Support, and Appeal tickets
- **Automatic Channel Creation**: Creates dedicated channels with proper permissions
- **Global Ticket Counter**: Persistent numbering system (ticket-username-001, 002, etc.)
- **Permission Management**: Automatically sets up channel permissions for ticket owners and staff

### 🔧 Administration
- **Easy Setup**: Simple slash command configuration
- **Role-Based Access**: Support staff role integration
- **Transcript Generation**: Automatic transcript creation with timestamps
- **Channel Management**: Close, reopen, and delete tickets with confirmation

### 🛡️ Security & Permissions
- **Administrator Required**: Bot requires Administrator permission for reliable operation
- **Role-Based Permissions**: Support staff and admin role verification
- **Owner Verification**: Ticket ownership validation

### 📊 Advanced Features
- **Transcript Logging**: Detailed conversation logs with attachments and reactions
- **Timezone Support**: UTC+08:00 (Malaysia/Singapore) timezone formatting
- **Embed Support**: Rich embed messages with proper formatting
- **File Attachment Tracking**: Complete record of uploaded files

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Discord Application with Bot Token
- Discord Server with Administrator permissions for the bot

## Installation

### Using Docker (Recommended)

1. **Clone the repository**
   ```bash
   git clone <your-repository-url>
   cd discord-ticket-bot
   ```

2. **Create environment file**
   ```bash
   # Create .env file for development
   echo "BOT_TOKEN=your_discord_bot_token_here" > .env
   
   # Or set environment variable for production
   export BOT_TOKEN=your_discord_bot_token_here
   ```

3. **Build and run with Docker**
   ```bash
   docker build -t discord-ticket-bot .
   docker run -e BOT_TOKEN=your_discord_bot_token_here discord-ticket-bot
   ```

### Manual Installation

1. **Clone and build**
   ```bash
   git clone <your-repository-url>
   cd discord-ticket-bot
   mvn clean package
   ```

2. **Run the bot**
   ```bash
   java -jar target/AWDCTicket-1.0-SNAPSHOT.jar
   ```

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

2. **Deploy ticket panel**
   ```
   /panel
   ```

### Available Commands

| Command | Description | Permission Required |
|---------|-------------|-------------------|
| `/help` | Show all available commands | Everyone |
| `/setup` | Configure the ticket system | Administrator |
| `/panel` | Send ticket panel to configured channel | Administrator |
| `/config` | View current bot configuration | Administrator |

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

### For Staff
1. **Managing Tickets**:
   - Use 🔒 Close button to close tickets
   - Choose from: Reopen, Generate Transcript, or Delete

2. **Transcript Generation**:
   - Automatically saves to configured transcript channel
   - Includes all messages, embeds, attachments, and reactions
   - Formatted with proper timestamps (UTC+08:00)

## Project Structure

```
src/main/java/com/discordticketbot/
├── bot/
│   ├── Main.java              # Application entry point
│   └── TicketBot.java         # Main bot class
├── config/
│   └── GuildConfig.java       # Server configuration
├── handlers/
│   ├── ConfigHandler.java     # Configuration display
│   ├── HelpHandler.java       # Help command
│   ├── PanelHandler.java      # Panel deployment
│   ├── SetupHandler.java      # Bot setup
│   └── TicketHandler.java     # Ticket management
├── listeners/
│   ├── ButtonListener.java    # Button interactions
│   ├── CommandListener.java   # Slash commands
│   ├── MessageListener.java   # Message handling
│   └── ReadyListener.java     # Bot initialization
└── utils/
    ├── CommandBuilder.java    # Command registration
    ├── PermissionUtil.java    # Permission checking
    ├── RoleParser.java        # Role parsing utility
    ├── TicketPanelUtil.java   # Panel creation
    └── TranscriptUtil.java    # Transcript generation
```

## Configuration Options

### Environment Variables
- `BOT_TOKEN` - Discord bot token (required)

### Guild Configuration (per server)
- **Category ID** - Where ticket channels are created
- **Panel Channel ID** - Where the ticket panel is displayed  
- **Transcript Channel ID** - Where transcripts are logged
- **Support Role IDs** - Staff roles that can manage tickets
- **Ticket Counter** - Persistent numbering for tickets

## Features in Detail

### Ticket Numbering System
- Global counter persists across bot restarts
- Format: `ticket-username-001`, `ticket-username-002`
- Continues numbering even after tickets are deleted
- Automatically detects existing tickets on startup

### Permission Management
- Ticket owner: Read, write, attach files
- Support staff: Full management permissions
- Public: No access
- Automatic permission updates when tickets are closed/reopened

### Transcript System
- Complete conversation history
- Attachment URLs and file sizes
- Embed content preservation
- Reaction tracking
- UTC+08:00 timezone formatting
- Discord timestamp conversion

## Troubleshooting

### Common Issues

1. **Bot not responding to commands**
   - Ensure bot has Administrator permission
   - Check if bot is online and properly invited

2. **Cannot create tickets**
   - Verify setup is complete with `/config`
   - Check category and channel permissions
   - Ensure panel channel is under the configured category

3. **Transcripts not generating**
   - Verify transcript channel exists and bot has access
   - Check file system permissions for transcript folder

4. **Permission errors**
   - Bot requires Administrator permission for reliable operation
   - Alternative: Grant specific permissions for channels, messages, and roles

### Error Messages
- ❌ Administrator permission required
- ❌ Configuration not found - use `/setup`
- ❌ Channel not found - reconfigure with `/setup`
- ❌ Already have open ticket - check existing channels

## Dependencies

- **JDA (Java Discord API)** 5.0.0-beta.20 - Discord bot framework
- **dotenv-java** 3.0.0 - Environment variable management
- **Maven Shade Plugin** - Creates executable JAR with dependencies

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is open source. Please check the license file for details.

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review the Discord bot setup requirements
3. Ensure proper permissions are configured
4. Create an issue with detailed information about the problem

---

**Note**: This bot requires Administrator permission to function properly due to the complex permission management and dynamic channel creation features required for a robust ticket system.
