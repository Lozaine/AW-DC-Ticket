# Discord Ticket Bot - Transcript Features

## Overview
The Discord Ticket Bot now includes enhanced transcript generation capabilities with HTML support and direct links.

## Features

### 1. Enhanced Transcript Generation
- **Text Transcripts**: Traditional plain text transcripts saved as .txt files
- **HTML Transcripts**: Beautiful, styled HTML transcripts with modern Discord-like appearance
- **Automatic Generation**: Both formats are generated simultaneously when staff/admin clicks the Transcript button

### 2. HTML Transcript Features
- **Modern Styling**: Discord-inspired dark theme with proper colors and typography
- **Responsive Design**: Works on both desktop and mobile devices
- **Rich Content**: Displays messages, embeds, attachments, reactions, and close request details
- **Professional Layout**: Clean, organized presentation suitable for record keeping

### 3. Direct Link System
- **Unique URLs**: Each transcript gets a unique identifier (UUID)
- **Web Access**: HTML transcripts are accessible via web browser
- **Railway Integration**: Uses `RAILWAY_PUBLIC_DOMAIN` environment variable for public access
- **Direct Link Button**: Staff can click "🌐 Open HTML Transcript" button to open in browser

## Configuration

### Environment Variables
```bash
# Required for public transcript access
RAILWAY_PUBLIC_DOMAIN=aw-dc-ticket-production.up.railway.app

# Bot token
BOT_TOKEN=your_discord_bot_token
```

### Application Properties
```properties
# Base URL for the application
app.base-url=https://aw-dc-ticket-production.up.railway.app

# Railway public domain for transcript links
railway.public.domain=aw-dc-ticket-production.up.railway.app
```

## Usage

### For Staff/Admins
1. **Generate Transcript**: Click the "📄 Transcript" button in any ticket channel
2. **Access Files**: Both .txt and .html files are saved to the transcript channel
3. **View HTML**: Click the "🌐 Open HTML Transcript" button to open in browser
4. **Direct Access**: Use the generated URL to access the transcript anytime

### For Users
- HTML transcripts are accessible via the direct link
- No authentication required for viewing transcripts
- Responsive design works on all devices

## Technical Details

### File Storage
- **Location**: `./transcripts/` directory
- **Naming**: `{channel}_{timestamp}_{uuid}.html`
- **Format**: UTF-8 encoded HTML with embedded CSS

### HTTP Server
- **Port**: 8080 (configurable)
- **Endpoints**:
  - `/transcript/{uuid}` - Serve HTML transcript
  - `/health` - Health check
  - `/` - Server info page

### Security Features
- **HTML Escaping**: Prevents XSS attacks
- **Unique IDs**: Each transcript has a UUID for access control
- **File Validation**: Only serves files from transcripts directory

## File Structure
```
transcripts/
├── ticket-user123_2024-01-15_14-30-25_abc123.html
├── ticket-user456_2024-01-15_15-45-12_def456.html
└── ticket-user789_2024-01-15_16-20-33_ghi789.html
```

## Troubleshooting

### Common Issues
1. **HTTP Server Not Starting**: Check if port 8080 is available
2. **Transcripts Not Accessible**: Verify `RAILWAY_PUBLIC_DOMAIN` is set correctly
3. **File Not Found**: Ensure transcript files exist in the transcripts directory

### Debug Commands
- Check server status: Visit `/health` endpoint
- List transcripts: Visit `/list` endpoint
- View server info: Visit `/` endpoint

## Future Enhancements
- Transcript search functionality
- Transcript archiving and cleanup
- User authentication for sensitive transcripts
- Export to PDF format
- Transcript analytics and statistics
