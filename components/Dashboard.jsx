import React, { useState, useEffect } from 'react';
import { 
  Server, 
  Ticket, 
  CheckCircle, 
  XCircle, 
  Clock, 
  TrendingUp,
  Activity,
  Calendar,
  Users,
  BarChart3
} from 'lucide-react';
import { format } from 'date-fns';

const Dashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/dashboard/overview');
      if (!response.ok) throw new Error('Failed to fetch data');
      const result = await response.json();
      setData(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="card">
              <div className="animate-pulse">
                <div className="h-4 bg-slate-700 rounded w-3/4 mb-2"></div>
                <div className="h-8 bg-slate-700 rounded w-1/2"></div>
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
          <XCircle className="w-12 h-12 text-red-400 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-white mb-2">Error Loading Dashboard</h3>
          <p className="text-gray-400 mb-4">{error}</p>
          <button 
            onClick={fetchDashboardData}
            className="btn btn-primary"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  const stats = [
    {
      title: 'Total Servers',
      value: data?.totalServers || 0,
      icon: Server,
      color: 'text-blue-400',
      bgColor: 'bg-blue-500/10',
      change: '+2 this week'
    },
    {
      title: 'Total Tickets',
      value: data?.totalTickets || 0,
      icon: Ticket,
      color: 'text-purple-400',
      bgColor: 'bg-purple-500/10',
      change: `+${data?.todayTickets || 0} today`
    },
    {
      title: 'Open Tickets',
      value: data?.openTickets || 0,
      icon: Clock,
      color: 'text-yellow-400',
      bgColor: 'bg-yellow-500/10',
      change: 'Active now'
    },
    {
      title: 'Closed Tickets',
      value: data?.closedTickets || 0,
      icon: CheckCircle,
      color: 'text-green-400',
      bgColor: 'bg-green-500/10',
      change: 'Resolved'
    }
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Dashboard</h1>
          <p className="text-gray-400 mt-1">Overview of your ticket system</p>
        </div>
        <button 
          onClick={fetchDashboardData}
          className="btn btn-primary"
        >
          <Activity className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div key={index} className="card">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-gray-400 mb-1">{stat.title}</p>
                  <p className="text-2xl font-bold text-white">{stat.value.toLocaleString()}</p>
                  <p className="text-xs text-gray-500 mt-1">{stat.change}</p>
                </div>
                <div className={`p-3 rounded-lg ${stat.bgColor}`}>
                  <Icon className={`w-6 h-6 ${stat.color}`} />
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Today's Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="card">
          <div className="card-header">
            <h3 className="card-title flex items-center gap-2">
              <Calendar className="w-5 h-5" />
              Today's Activity
            </h3>
            <p className="card-subtitle">Last 24 hours</p>
          </div>
          
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
              <div className="flex items-center gap-3">
                <div className="w-2 h-2 bg-green-400 rounded-full"></div>
                <span className="text-white">New Tickets Created</span>
              </div>
              <span className="text-2xl font-bold text-green-400">{data?.todayTickets || 0}</span>
            </div>
            
            <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
              <div className="flex items-center gap-3">
                <div className="w-2 h-2 bg-blue-400 rounded-full"></div>
                <span className="text-white">Active Servers</span>
              </div>
              <span className="text-2xl font-bold text-blue-400">{data?.totalServers || 0}</span>
            </div>
            
            <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
              <div className="flex items-center gap-3">
                <div className="w-2 h-2 bg-yellow-400 rounded-full"></div>
                <span className="text-white">Pending Tickets</span>
              </div>
              <span className="text-2xl font-bold text-yellow-400">{data?.openTickets || 0}</span>
            </div>
          </div>
        </div>

        {/* Recent Activity */}
        <div className="card">
          <div className="card-header">
            <h3 className="card-title flex items-center gap-2">
              <Activity className="w-5 h-5" />
              Recent Activity
            </h3>
            <p className="card-subtitle">Latest ticket updates</p>
          </div>
          
          <div className="space-y-3">
            {data?.recentActivity?.slice(0, 6).map((activity, index) => (
              <div key={index} className="flex items-center gap-3 p-3 bg-slate-800 rounded-lg">
                <div className={`w-2 h-2 rounded-full ${
                  activity.status === 'open' ? 'bg-green-400' :
                  activity.status === 'closed' ? 'bg-red-400' :
                  activity.status === 'deleted' ? 'bg-gray-400' : 'bg-yellow-400'
                }`}></div>
                <div className="flex-1 min-w-0">
                  <p className="text-white text-sm font-medium truncate">
                    {activity.channel_name}
                  </p>
                  <p className="text-gray-400 text-xs">
                    {activity.ticket_type} • {format(new Date(activity.created_at), 'MMM dd, HH:mm')}
                  </p>
                </div>
                <span className={`status-badge status-${activity.status}`}>
                  {activity.status}
                </span>
              </div>
            ))}
            
            {(!data?.recentActivity || data.recentActivity.length === 0) && (
              <div className="text-center py-8 text-gray-400">
                <Activity className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>No recent activity</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Quick Stats */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title flex items-center gap-2">
            <BarChart3 className="w-5 h-5" />
            Quick Statistics
          </h3>
          <p className="card-subtitle">System overview</p>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center">
            <div className="text-3xl font-bold text-blue-400 mb-2">
              {data?.totalTickets ? Math.round((data.closedTickets / data.totalTickets) * 100) : 0}%
            </div>
            <p className="text-gray-400">Resolution Rate</p>
          </div>
          
          <div className="text-center">
            <div className="text-3xl font-bold text-green-400 mb-2">
              {data?.openTickets || 0}
            </div>
            <p className="text-gray-400">Active Tickets</p>
          </div>
          
          <div className="text-center">
            <div className="text-3xl font-bold text-purple-400 mb-2">
              {data?.totalServers || 0}
            </div>
            <p className="text-gray-400">Connected Servers</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;