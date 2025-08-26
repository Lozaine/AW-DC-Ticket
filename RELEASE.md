# Release Notes — v1.4.0 (2025-08-27)

## Highlights
- 🌐 **Complete HTML Transcript System** with direct web access
- 🚀 **Built-in HTTP Server** for serving transcripts
- 🔗 **Direct Link System** for easy transcript sharing
- 📱 **Responsive Design** for mobile and desktop viewing
- 🎨 **Professional Styling** with Discord-inspired theme

## What's New

### 1) Enhanced Transcript Generation System
- **Dual Format Support**: Generates both text (.txt) and HTML (.html) transcripts simultaneously
- **HTML Transcripts**: Beautiful, modern Discord-inspired styling with responsive design
- **Rich Content Display**: Shows messages, embeds, attachments, reactions, and close request details
- **Professional Layout**: Clean, organized presentation suitable for record keeping and documentation

### 2) Direct Link System
- **Unique URLs**: Each transcript gets a UUID for secure access control
- **Web Access**: HTML transcripts accessible via web browser from anywhere
- **Railway Integration**: Uses `RAILWAY_PUBLIC_DOMAIN` environment variable for public access
- **Direct Link Button**: Staff can click "🌐 Open HTML Transcript" button to open in browser

### 3) Built-in HTTP Server
- **Port 8080**: Configurable HTTP server runs alongside the Discord bot
- **Endpoints**: 
  - `/transcript/{uuid}` - Serve HTML transcripts
  - `/health` - Health check
  - `/` - Server info page
- **Automatic Startup**: Server starts automatically when bot launches

### 4) Enhanced User Experience
- **Staff Access Control**: Only staff members can generate transcripts
- **Visual Feedback**: Clear indication when HTML transcript is available
- **Mobile Friendly**: Responsive design works on all devices
- **Security Features**: HTML escaping prevents XSS attacks

## Technical Implementation

### New Files
- `HttpServerUtil.java` - Simple HTTP server for serving transcripts
- `TRANSCRIPT_FEATURES.md` - Comprehensive documentation

### Enhanced Files
- `TranscriptUtil.java` - Added HTML generation, direct link functionality, and file management
- `TicketHandler.java` - Updated to generate both formats and include Direct Link button
- `Application.java` - Modified to start HTTP server alongside Discord bot
- `application.properties` - Added Railway domain configuration

### Key Features
- **File Storage**: `./transcripts/` directory with unique naming convention
- **Environment Variables**: `RAILWAY_PUBLIC_DOMAIN` for public access
- **Error Handling**: Graceful fallbacks and comprehensive error logging
- **Resource Management**: Proper cleanup and shutdown procedures

## Configuration

### Environment Variables
```bash
RAILWAY_PUBLIC_DOMAIN=aw-dc-ticket-production.up.railway.app
BOT_TOKEN=your_discord_bot_token
```

### Application Properties
```properties
app.base-url=https://aw-dc-ticket-production.up.railway.app
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

## Commands
- Existing transcript generation via "📄 Transcript" button (enhanced)
- No new slash commands - all functionality integrated into existing UI

## Upgrade Notes
- **No DB schema changes** required
- **HTTP server starts automatically** on port 8080
- **Environment variable setup** needed for public access
- **Backward compatible** - existing text transcripts still work

## Files Changed (Key)
- **New**: `HttpServerUtil.java`, `TRANSCRIPT_FEATURES.md`
- **Enhanced**: `TranscriptUtil.java`, `TicketHandler.java`, `Application.java`
- **Config**: `application.properties`

## Verification
- **Build**: `mvn clean compile` should succeed
- **HTTP Server**: Check `/health` endpoint after bot startup
- **Transcript Generation**: Generate transcript in any ticket channel
- **Direct Links**: Click "🌐 Open HTML Transcript" button to verify web access

## Security Features
- **HTML Escaping**: Prevents XSS attacks
- **Unique IDs**: Each transcript has UUID for access control
- **File Validation**: Only serves files from transcripts directory
- **No Authentication Required**: Public access for transcript viewing

## Future Enhancements
- Transcript search functionality
- Transcript archiving and cleanup
- User authentication for sensitive transcripts
- Export to PDF format
- Transcript analytics and statistics
