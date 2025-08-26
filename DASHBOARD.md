# Discord Ticket Bot Web Dashboard

This document provides instructions for setting up and using the web dashboard for the Discord Ticket Bot.

## Features

- **Server Management**: View and manage all servers where the bot is installed
- **Configuration Interface**: Configure ticket settings through a user-friendly web interface
- **Ticket Management**: View, search, and manage tickets from all your servers
- **Analytics**: View statistics and performance metrics for your ticket system
- **Discord OAuth2 Integration**: Secure login with Discord account

## Setup Instructions

### Prerequisites

- Discord Bot Token (already configured)
- Discord OAuth2 Application (same as your bot)
- PostgreSQL Database (already configured)
- Railway.app account (or other hosting platform)

### Environment Variables

The following environment variables need to be set in your Railway project:

```
BOT_TOKEN=your_discord_bot_token
DATABASE_URL=postgresql://username:password@host:port/database
PUBLIC_BASE_URL=https://aw-dc-ticket-production.up.railway.app
BOT_CLIENT_ID=your_discord_application_client_id
BOT_CLIENT_SECRET=your_discord_application_client_secret
```

### Discord Developer Portal Configuration

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications)
2. Select your bot application
3. Go to the "OAuth2" section
4. Add a redirect URL: `https://aw-dc-ticket-production.up.railway.app/login/oauth2/code/discord`
5. Save changes

### Deployment

1. Push your code to your GitHub repository
2. Connect your Railway project to your GitHub repository
3. Set the required environment variables in Railway
4. Deploy the application

## Usage Guide

### Accessing the Dashboard

1. Navigate to `https://aw-dc-ticket-production.up.railway.app`
2. Click "Login with Discord"
3. Authorize the application to access your Discord account

### Managing Servers

1. After logging in, you'll see a list of servers where:
   - The bot is installed
   - You have administrator permissions
2. Click "Manage" on a server to access its settings

### Configuring a Server

1. In the server management page, you can configure:
   - Ticket category
   - Panel channel
   - Transcript channel
   - Error log channel
   - Support roles
2. Save your changes to update the configuration

### Managing Tickets

1. Go to the "Tickets" tab for a server
2. View all tickets for that server
3. Filter tickets by status, type, or search for specific tickets
4. Perform actions like viewing, closing, or reopening tickets

### Viewing Analytics

1. Go to the "Analytics" tab for a server
2. View statistics such as:
   - Total tickets
   - Open vs. closed tickets
   - Average response time
   - Ticket distribution by type

## Troubleshooting

### Common Issues

- **Login Fails**: Ensure your OAuth2 redirect URL is correctly set in the Discord Developer Portal
- **Cannot See Servers**: Make sure you have administrator permissions on the server and the bot is installed
- **Configuration Not Saving**: Check database connection and permissions

### Support

If you encounter any issues with the dashboard, please:
1. Check the server logs for error messages
2. Ensure all environment variables are correctly set
3. Verify your Discord application settings

## Security Considerations

- The dashboard uses Discord OAuth2 for authentication
- Only users with administrator permissions on a server can manage that server
- All communication is encrypted with HTTPS
- Bot token and database credentials are securely stored as environment variables