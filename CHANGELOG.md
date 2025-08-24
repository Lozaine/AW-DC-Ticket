# Changelog

All notable changes to this project are documented in this file following [Keep a Changelog](https://keepachangelog.com/) guidelines, and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.2.0] – 2025-08-24

### Added
- 🆕 **Full Ticket Logging System**  
  - `TicketLogDAO.java` now records ticket lifecycle events—creation, closure, reopening, and deletion—in the `ticket_logs` table.
-  **Server-level Ticket Stats Command**  
  - Added a `/stats` slash command via `StatsHandler.java` to display real-time ticket counts (total, open, closed, deleted).
-  **Recent Tickets Retrieval**  
  - Leveraged `getRecentTickets(...)` for future enhancements like ticket history embeds or paginated views.

### Changed
-  **Audit Trail Enhancement**  
  - All ticket actions (create, close, reopen, delete) are now logged with timestamp and user info for improved transparency.
-  **Live Analytics for Admins**  
  - `/stats` command empowers admins with instant insights into ticket activity and trends.

### Modified
- `TicketLogDAO.java` — Added logging methods (`logTicketCreated`, `logTicketClosed`, `logTicketReopened`, `logTicketDeleted`), `getGuildTicketStats(...)`, and `getRecentTickets(...)`.
- **New file:** `StatsHandler.java` — Handles the `/stats` command and formats reply.
- `TicketHandler.java` — Updated to include logging calls for ticket creation, closure, reopening, and deletion.

### Deployment Notes
1. Add `StatsHandler.java` into your handlers package.
2. Register the `/stats` slash command (e.g., via JDA’s `updateCommands()` or `upsertCommand()`).
3. Deploy the bot and test `/stats` in a server to confirm metrics are showing correctly.

---

## [1.1.0] – Previous release highlights (for reference)

### Added
- PostgreSQL integration and persistence support.
- `DatabaseManager` using HikariCP and automatic schema creation.
- `GuildConfigDAO` for persistent guild settings.
- Ticket counters and guild settings persist across restarts.

### Changed
- Main and setup handlers updated to load/save configs from the database.
- Enhanced `.env` support for local development.

---

## [Unreleased]
- _Next planned changes go here…_
