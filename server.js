import express from 'express';
import cors from 'cors';
import { Client } from 'pg';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3001;

// Middleware
app.use(cors());
app.use(express.json());

// Serve static files from dist directory
app.use(express.static(path.join(__dirname, 'dist')));

// Database connection
const dbConfig = {
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
};

// API Routes

// Dashboard overview
app.get('/api/dashboard/overview', async (req, res) => {
  const client = new Client(dbConfig);
  
  try {
    await client.connect();
    
    // Get total servers
    const serversResult = await client.query('SELECT COUNT(DISTINCT guild_id) as total FROM guild_configs');
    const totalServers = serversResult.rows[0].total;
    
    // Get ticket statistics
    const ticketsResult = await client.query(`
      SELECT 
        COUNT(*) as total_tickets,
        COUNT(CASE WHEN status = 'open' THEN 1 END) as open_tickets,
        COUNT(CASE WHEN status = 'closed' THEN 1 END) as closed_tickets,
        COUNT(CASE WHEN status = 'deleted' THEN 1 END) as deleted_tickets
      FROM ticket_logs 
      WHERE ticket_type NOT IN ('close_request', 'close_denied')
    `);
    
    const stats = ticketsResult.rows[0];
    
    // Get today's activity (last 24 hours)
    const todayResult = await client.query(`
      SELECT COUNT(*) as today_tickets
      FROM ticket_logs 
      WHERE created_at >= NOW() - INTERVAL '24 hours'
      AND ticket_type NOT IN ('close_request', 'close_denied')
    `);
    
    const todayTickets = todayResult.rows[0].today_tickets;
    
    // Get recent activity
    const recentResult = await client.query(`
      SELECT 
        tl.channel_name,
        tl.ticket_type,
        tl.status,
        tl.created_at,
        tl.closed_at,
        gc.guild_id,
        (SELECT COUNT(*) FROM guild_configs WHERE guild_id = tl.guild_id) as guild_exists
      FROM ticket_logs tl
      LEFT JOIN guild_configs gc ON tl.guild_id = gc.guild_id
      WHERE tl.ticket_type NOT IN ('close_request', 'close_denied')
      ORDER BY tl.created_at DESC 
      LIMIT 10
    `);
    
    res.json({
      totalServers: parseInt(totalServers),
      totalTickets: parseInt(stats.total_tickets),
      openTickets: parseInt(stats.open_tickets),
      closedTickets: parseInt(stats.closed_tickets),
      deletedTickets: parseInt(stats.deleted_tickets),
      todayTickets: parseInt(todayTickets),
      recentActivity: recentResult.rows
    });
    
  } catch (error) {
    console.error('Database error:', error);
    res.status(500).json({ error: 'Database connection failed' });
  } finally {
    await client.end();
  }
});

// Get all servers
app.get('/api/servers', async (req, res) => {
  const client = new Client(dbConfig);
  
  try {
    await client.connect();
    
    const result = await client.query(`
      SELECT 
        gc.guild_id,
        gc.category_id,
        gc.panel_channel_id,
        gc.transcript_channel_id,
        gc.error_log_channel_id,
        gc.ticket_counter,
        gc.created_at,
        gc.updated_at,
        COUNT(sr.role_id) as support_roles_count,
        (
          SELECT COUNT(*) 
          FROM ticket_logs tl 
          WHERE tl.guild_id = gc.guild_id 
          AND tl.ticket_type NOT IN ('close_request', 'close_denied')
        ) as total_tickets,
        (
          SELECT COUNT(*) 
          FROM ticket_logs tl 
          WHERE tl.guild_id = gc.guild_id 
          AND tl.status = 'open'
          AND tl.ticket_type NOT IN ('close_request', 'close_denied')
        ) as open_tickets
      FROM guild_configs gc
      LEFT JOIN support_roles sr ON gc.guild_id = sr.guild_id
      GROUP BY gc.guild_id, gc.category_id, gc.panel_channel_id, gc.transcript_channel_id, 
               gc.error_log_channel_id, gc.ticket_counter, gc.created_at, gc.updated_at
      ORDER BY gc.updated_at DESC
    `);
    
    res.json(result.rows);
    
  } catch (error) {
    console.error('Database error:', error);
    res.status(500).json({ error: 'Database connection failed' });
  } finally {
    await client.end();
  }
});

// Get all tickets with pagination
app.get('/api/tickets', async (req, res) => {
  const client = new Client(dbConfig);
  const page = parseInt(req.query.page) || 1;
  const limit = parseInt(req.query.limit) || 50;
  const offset = (page - 1) * limit;
  const status = req.query.status;
  const guildId = req.query.guild_id;
  
  try {
    await client.connect();
    
    let whereClause = "WHERE ticket_type NOT IN ('close_request', 'close_denied')";
    const params = [];
    let paramCount = 0;
    
    if (status) {
      paramCount++;
      whereClause += ` AND status = $${paramCount}`;
      params.push(status);
    }
    
    if (guildId) {
      paramCount++;
      whereClause += ` AND guild_id = $${paramCount}`;
      params.push(guildId);
    }
    
    // Get total count
    const countResult = await client.query(`
      SELECT COUNT(*) as total 
      FROM ticket_logs 
      ${whereClause}
    `, params);
    
    const totalTickets = parseInt(countResult.rows[0].total);
    
    // Get tickets with pagination
    const ticketsResult = await client.query(`
      SELECT 
        id,
        guild_id,
        channel_id,
        channel_name,
        owner_id,
        ticket_type,
        ticket_number,
        created_at,
        closed_at,
        closed_by,
        status
      FROM ticket_logs 
      ${whereClause}
      ORDER BY created_at DESC 
      LIMIT $${paramCount + 1} OFFSET $${paramCount + 2}
    `, [...params, limit, offset]);
    
    res.json({
      tickets: ticketsResult.rows,
      pagination: {
        page,
        limit,
        total: totalTickets,
        totalPages: Math.ceil(totalTickets / limit)
      }
    });
    
  } catch (error) {
    console.error('Database error:', error);
    res.status(500).json({ error: 'Database connection failed' });
  } finally {
    await client.end();
  }
});

// Get ticket analytics
app.get('/api/analytics', async (req, res) => {
  const client = new Client(dbConfig);
  
  try {
    await client.connect();
    
    // Daily ticket creation for last 30 days
    const dailyStatsResult = await client.query(`
      SELECT 
        DATE(created_at) as date,
        COUNT(*) as tickets_created,
        COUNT(CASE WHEN status = 'closed' THEN 1 END) as tickets_closed
      FROM ticket_logs 
      WHERE created_at >= NOW() - INTERVAL '30 days'
      AND ticket_type NOT IN ('close_request', 'close_denied')
      GROUP BY DATE(created_at)
      ORDER BY date DESC
    `);
    
    // Ticket types distribution
    const typeStatsResult = await client.query(`
      SELECT 
        ticket_type,
        COUNT(*) as count
      FROM ticket_logs 
      WHERE ticket_type NOT IN ('close_request', 'close_denied')
      GROUP BY ticket_type
      ORDER BY count DESC
    `);
    
    // Status distribution
    const statusStatsResult = await client.query(`
      SELECT 
        status,
        COUNT(*) as count
      FROM ticket_logs 
      WHERE ticket_type NOT IN ('close_request', 'close_denied')
      GROUP BY status
      ORDER BY count DESC
    `);
    
    // Top servers by ticket volume
    const serverStatsResult = await client.query(`
      SELECT 
        guild_id,
        COUNT(*) as ticket_count
      FROM ticket_logs 
      WHERE ticket_type NOT IN ('close_request', 'close_denied')
      GROUP BY guild_id
      ORDER BY ticket_count DESC
      LIMIT 10
    `);
    
    res.json({
      dailyStats: dailyStatsResult.rows,
      typeStats: typeStatsResult.rows,
      statusStats: statusStatsResult.rows,
      serverStats: serverStatsResult.rows
    });
    
  } catch (error) {
    console.error('Database error:', error);
    res.status(500).json({ error: 'Database connection failed' });
  } finally {
    await client.end();
  }
});

// Get server details
app.get('/api/servers/:guildId', async (req, res) => {
  const client = new Client(dbConfig);
  const { guildId } = req.params;
  
  try {
    await client.connect();
    
    // Get server config
    const configResult = await client.query(`
      SELECT * FROM guild_configs WHERE guild_id = $1
    `, [guildId]);
    
    if (configResult.rows.length === 0) {
      return res.status(404).json({ error: 'Server not found' });
    }
    
    // Get support roles
    const rolesResult = await client.query(`
      SELECT role_id FROM support_roles WHERE guild_id = $1
    `, [guildId]);
    
    // Get server ticket stats
    const statsResult = await client.query(`
      SELECT 
        COUNT(*) as total_tickets,
        COUNT(CASE WHEN status = 'open' THEN 1 END) as open_tickets,
        COUNT(CASE WHEN status = 'closed' THEN 1 END) as closed_tickets,
        COUNT(CASE WHEN status = 'deleted' THEN 1 END) as deleted_tickets
      FROM ticket_logs 
      WHERE guild_id = $1 
      AND ticket_type NOT IN ('close_request', 'close_denied')
    `, [guildId]);
    
    res.json({
      config: configResult.rows[0],
      supportRoles: rolesResult.rows.map(r => r.role_id),
      stats: statsResult.rows[0]
    });
    
  } catch (error) {
    console.error('Database error:', error);
    res.status(500).json({ error: 'Database connection failed' });
  } finally {
    await client.end();
  }
});

// Health check
app.get('/api/health', async (req, res) => {
  const client = new Client(dbConfig);
  
  try {
    await client.connect();
    await client.query('SELECT 1');
    res.json({ status: 'healthy', database: 'connected' });
  } catch (error) {
    res.status(500).json({ status: 'unhealthy', database: 'disconnected', error: error.message });
  } finally {
    await client.end();
  }
});

// Serve React app for all other routes
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'dist', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`🚀 AW DC Ticket Dashboard server running on port ${PORT}`);
  console.log(`📊 Dashboard available at http://localhost:${PORT}`);
});