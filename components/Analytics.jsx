import React, { useState, useEffect } from 'react';
import { 
  BarChart3, 
  TrendingUp, 
  PieChart, 
  Calendar,
  RefreshCw,
  Download,
  Server,
  Ticket,
  Clock,
  CheckCircle
} from 'lucide-react';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  CartesianGrid, 
  Tooltip, 
  ResponsiveContainer,
  PieChart as RechartsPieChart,
  Cell,
  LineChart,
  Line,
  Area,
  AreaChart
} from 'recharts';
import { format, subDays } from 'date-fns';

const Analytics = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timeRange, setTimeRange] = useState('30');

  useEffect(() => {
    fetchAnalytics();
  }, []);

  const fetchAnalytics = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/analytics');
      if (!response.ok) throw new Error('Failed to fetch analytics');
      const result = await response.json();
      setData(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const COLORS = {
    primary: '#3b82f6',
    secondary: '#8b5cf6',
    success: '#10b981',
    warning: '#f59e0b',
    danger: '#ef4444',
    info: '#06b6d4'
  };

  const PIE_COLORS = [COLORS.primary, COLORS.secondary, COLORS.success, COLORS.warning, COLORS.danger];

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold text-white">Analytics</h1>
          <div className="spinner"></div>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="card h-80">
              <div className="animate-pulse">
                <div className="h-4 bg-slate-700 rounded w-1/3 mb-4"></div>
                <div className="h-64 bg-slate-700 rounded"></div>
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card">
        <div className="text-center py-8">
          <BarChart3 className="w-12 h-12 text-red-400 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-white mb-2">Error Loading Analytics</h3>
          <p className="text-gray-400 mb-4">{error}</p>
          <button onClick={fetchAnalytics} className="btn btn-primary">
            Try Again
          </button>
        </div>
      </div>
    );
  }

  // Process daily stats for chart
  const dailyChartData = data?.dailyStats?.slice(0, 30).reverse().map(stat => ({
    date: format(new Date(stat.date), 'MMM dd'),
    created: parseInt(stat.tickets_created),
    closed: parseInt(stat.tickets_closed)
  })) || [];

  // Process type stats for pie chart
  const typeChartData = data?.typeStats?.map((stat, index) => ({
    name: stat.ticket_type,
    value: parseInt(stat.count),
    color: PIE_COLORS[index % PIE_COLORS.length]
  })) || [];

  // Process status stats for pie chart
  const statusChartData = data?.statusStats?.map((stat, index) => ({
    name: stat.status,
    value: parseInt(stat.count),
    color: PIE_COLORS[index % PIE_COLORS.length]
  })) || [];

  const totalTickets = data?.statusStats?.reduce((sum, stat) => sum + parseInt(stat.count), 0) || 0;
  const totalServers = data?.serverStats?.length || 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Analytics</h1>
          <p className="text-gray-400 mt-1">Detailed insights and statistics</p>
        </div>
        <div className="flex gap-2">
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value)}
            className="form-input"
          >
            <option value="7">Last 7 days</option>
            <option value="30">Last 30 days</option>
            <option value="90">Last 90 days</option>
          </select>
          <button onClick={fetchAnalytics} className="btn btn-primary">
            <RefreshCw className="w-4 h-4" />
            Refresh
          </button>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="card">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-blue-500/10 rounded-lg">
              <Ticket className="w-6 h-6 text-blue-400" />
            </div>
            <div>
              <p className="text-sm text-gray-400">Total Tickets</p>
              <p className="text-2xl font-bold text-white">{totalTickets.toLocaleString()}</p>
            </div>
          </div>
        </div>
        
        <div className="card">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-green-500/10 rounded-lg">
              <CheckCircle className="w-6 h-6 text-green-400" />
            </div>
            <div>
              <p className="text-sm text-gray-400">Resolution Rate</p>
              <p className="text-2xl font-bold text-white">
                {totalTickets > 0 ? Math.round((data?.statusStats?.find(s => s.status === 'closed')?.count || 0) / totalTickets * 100) : 0}%
              </p>
            </div>
          </div>
        </div>
        
        <div className="card">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-purple-500/10 rounded-lg">
              <Server className="w-6 h-6 text-purple-400" />
            </div>
            <div>
              <p className="text-sm text-gray-400">Active Servers</p>
              <p className="text-2xl font-bold text-white">{totalServers}</p>
            </div>
          </div>
        </div>
        
        <div className="card">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-yellow-500/10 rounded-lg">
              <Clock className="w-6 h-6 text-yellow-400" />
            </div>
            <div>
              <p className="text-sm text-gray-400">Avg. Daily Tickets</p>
              <p className="text-2xl font-bold text-white">
                {dailyChartData.length > 0 ? Math.round(dailyChartData.reduce((sum, day) => sum + day.created, 0) / dailyChartData.length) : 0}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Daily Activity Chart */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title flex items-center gap-2">
              <BarChart3 className="w-5 h-5" />
              Daily Activity
            </h3>
            <p className="card-subtitle">Tickets created vs closed over time</p>
          </div>
          
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={dailyChartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis 
                  dataKey="date" 
                  stroke="#94a3b8"
                  fontSize={12}
                />
                <YAxis stroke="#94a3b8" fontSize={12} />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: '#1e293b', 
                    border: '1px solid #334155',
                    borderRadius: '8px',
                    color: '#e2e8f0'
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="created"
                  stackId="1"
                  stroke={COLORS.primary}
                  fill={COLORS.primary}
                  fillOpacity={0.6}
                  name="Created"
                />
                <Area
                  type="monotone"
                  dataKey="closed"
                  stackId="2"
                  stroke={COLORS.success}
                  fill={COLORS.success}
                  fillOpacity={0.6}
                  name="Closed"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Ticket Types Distribution */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title flex items-center gap-2">
              <PieChart className="w-5 h-5" />
              Ticket Types
            </h3>
            <p className="card-subtitle">Distribution by ticket type</p>
          </div>
          
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <RechartsPieChart>
                <Pie
                  data={typeChartData}
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  dataKey="value"
                  label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                >
                  {typeChartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: '#1e293b', 
                    border: '1px solid #334155',
                    borderRadius: '8px',
                    color: '#e2e8f0'
                  }}
                />
              </RechartsPieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Status Distribution */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title flex items-center gap-2">
              <TrendingUp className="w-5 h-5" />
              Ticket Status
            </h3>
            <p className="card-subtitle">Current status distribution</p>
          </div>
          
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={statusChartData} layout="horizontal">
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis type="number" stroke="#94a3b8" fontSize={12} />
                <YAxis 
                  type="category" 
                  dataKey="name" 
                  stroke="#94a3b8" 
                  fontSize={12}
                  width={80}
                />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: '#1e293b', 
                    border: '1px solid #334155',
                    borderRadius: '8px',
                    color: '#e2e8f0'
                  }}
                />
                <Bar dataKey="value" fill={COLORS.primary} radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Top Servers */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title flex items-center gap-2">
              <Server className="w-5 h-5" />
              Top Servers
            </h3>
            <p className="card-subtitle">Servers by ticket volume</p>
          </div>
          
          <div className="space-y-3">
            {data?.serverStats?.slice(0, 8).map((server, index) => (
              <div key={server.guild_id} className="flex items-center justify-between p-3 bg-slate-800 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 bg-blue-500/10 rounded-lg flex items-center justify-center">
                    <span className="text-sm font-bold text-blue-400">#{index + 1}</span>
                  </div>
                  <div>
                    <code className="text-sm text-white">{server.guild_id}</code>
                    <p className="text-xs text-gray-400">Guild ID</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-lg font-bold text-white">{server.ticket_count}</p>
                  <p className="text-xs text-gray-400">tickets</p>
                </div>
              </div>
            ))}
            
            {(!data?.serverStats || data.serverStats.length === 0) && (
              <div className="text-center py-8 text-gray-400">
                <Server className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>No server data available</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">Summary Statistics</h3>
          <p className="card-subtitle">Key performance indicators</p>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center p-6 bg-slate-800 rounded-lg">
            <div className="text-3xl font-bold text-blue-400 mb-2">
              {data?.dailyStats?.length || 0}
            </div>
            <p className="text-gray-400">Days of Data</p>
          </div>
          
          <div className="text-center p-6 bg-slate-800 rounded-lg">
            <div className="text-3xl font-bold text-green-400 mb-2">
              {Math.round(((data?.statusStats?.find(s => s.status === 'closed')?.count || 0) / totalTickets) * 100) || 0}%
            </div>
            <p className="text-gray-400">Success Rate</p>
          </div>
          
          <div className="text-center p-6 bg-slate-800 rounded-lg">
            <div className="text-3xl font-bold text-purple-400 mb-2">
              {data?.statusStats?.find(s => s.status === 'open')?.count || 0}
            </div>
            <p className="text-gray-400">Active Tickets</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;