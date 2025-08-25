# Release Notes — v1.3.0 (2025-08-25)

## Highlights
- Automatic ticket cleanup policies (daily scheduler + admin command)
- Enhanced transcript formatting with optional HTML output
- Ticket assignment to specific staff via slash command

## What’s New
### 1) Automatic Ticket Cleanup
- Daily background task cleans up old records based on per-guild settings.
- Admins can configure retention using `/cleanup`:
    - logs_days (default 30): retain closed/deleted ticket logs
    - requests_days (default 30): retain processed close-requests
    - transcript_html (default true): toggle HTML transcript generation

Implementation:
- TicketBot.startCleanupScheduler() runs a daily job invoking:
    - TicketLogDAO.cleanupOldTicketLogs(days)
    - CloseRequestDAO.cleanupOldCloseRequests(days)
- Command handler: CleanupHandler (registered in CommandListener)
- Config stored per guild in GuildConfig:
    - cleanupTicketLogsDays
    - cleanupCloseRequestsDays
    - transcriptHtmlEnabled

### 2) Enhanced Transcripts
- HTML transcripts with styled layout, author tags, timestamps, embeds, reactions, attachments, and close-request details.
- When enabled, both TXT and HTML files are uploaded to the transcript channel.

Implementation:
- TranscriptUtil.createHtmlTranscript(...)
- TranscriptUtil.saveHtmlTranscriptToFile(...)
- TicketHandler.generateAndSendTranscript(...) now attaches HTML when GuildConfig.transcriptHtmlEnabled is true.

### 3) Ticket Assignment to Staff
- New `/assign member:@User` command (staff-only) to assign tickets to specific team members.
- Grants explicit channel permissions to the assignee and posts an assignment embed.

Implementation:
- AssignmentHandler (wired in CommandListener)

## Commands
- /cleanup logs_days:<int?> requests_days:<int?> transcript_html:<bool?> (Admin)
- /assign member:<user> (Staff)
- Existing commands unaffected; help updated to reflect new features.

## Upgrade Notes
- No DB schema changes required beyond existing ticket_logs and close_requests tables.
- Ensure the bot has sufficient permissions to manage channel overrides for assignments.
- Commands are registered globally by ReadyListener via CommandBuilder.

## Files Changed (Key)
- Config: GuildConfig (new cleanup + transcript flags)
- Commands: CommandBuilder, CommandListener
- Handlers: CleanupHandler, AssignmentHandler, TicketHandler (transcript wiring)
- Bot: TicketBot (daily scheduler)
- Help: HelpHandler (feature list updated)

## Verification
- Build: mvn -DskipTests package should succeed
- Smoke tests:
    - Run `/cleanup transcript_html:true`, generate transcript -> expect both TXT and HTML
    - Run `/assign @Staff` inside a ticket -> assignee gains access and embed posts
    - Wait for daily cleanup or run `/cleanup logs_days:30 requests_days:30` to trigger immediate cleanups
