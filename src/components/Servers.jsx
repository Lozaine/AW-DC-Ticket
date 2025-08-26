import React, { useState, useEffect } from 'react';
import { 
  Server, 
  Users, 
  Ticket, 
  Settings, 
  Eye, 
  RefreshCw,
  Search,
  Filter,
  CheckCircle,
  XCircle,
  Clock,
  Database
} from 'lucide-react';
import { format } from 'date-fns';

const Servers = () => {
  const [servers, setServers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedServer, setSelectedServer] = useState(null);

  useEffect(() => {
    fetchServers();
  }, []);

  const fetchServers = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/servers');
      if (!response.ok) throw new Error('Failed to fetch servers');
      const data = await response.json();
      setServers(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchServerDetails = async (guildId) => {
    try {
      const response = await fetch(`/api/servers/${guildId}`);
      if (!response.ok) throw new Error('Failed to fetch server details');
      const data = await response.json();
      setSelectedServer(data);
    } catch (err) {
      console.error('Error fetching server details:', err);
    }
  };

  const filteredServers = servers.filter(server =>
    server.guild_id.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold text-white">Servers</h1>
          <div className="spinner"></div>
        </div>
        <div className="grid gap-4">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="card">
              <div className="animate-pulse">
                <div className="h-4 bg-slate-700 rounded w-1/4 mb-2"></div>
                <div className="h-6 bg-slate-700 rounded w-1/2"></div>
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
          <h3 className="text-lg font-semibold text-white mb-2">Error Loading Servers</h3>
          <p className="text-gray-400 mb-4">{error}</p>
          <button onClick={fetchServers} className="btn btn-primary">
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Servers</h1>
          <p className="text-gray-400 mt-1">Manage connected Discord servers</p>
        </div>
        <button onClick={fetchServers} className="btn btn-primary">
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {/* Search and Filters */}
      <div className="flex gap-4">
        <div className="flex-1 relative">
          <Search className="w-5 h-5 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Search servers by Guild ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="form-input pl-10"
          />
        </div>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="card">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-500/10 rounded-lg">
              <Server className="w-5 h-5 text-blue-400" />
            </div>
            <div>
              <p className="text-sm text-gray-400">Total Servers</p>
              <p className="text-xl font-bold text-white">{servers.length}</p>
            </div>
          </div>
        </div>
        
        <div className="card">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-green-500/10 rounded-lg">
              <CheckCircle className="w-5 h-5 text-green-400" />
            </div>
            <div>
              <p className="text-sm text-gray-400">Active Servers</p>
              <p className="text-xl font-bold text-white">
                {servers.filter(s => s.total_tickets > 0).length}
              </p>
            </div>
          </div>
        </div>
        
        <div className="card">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-purple-500/10 rounded-lg">
              <Ticket className="w-5 h-5 text-purple-400" />
            </div>
            <div>
              <p className="text-sm text-gray-400">Total Tickets</p>
              <p className="text-xl font-bold text-white">
                {servers.reduce((sum, s) => sum + parseInt(s.total_tickets || 0), 0)}
              </p>
            </div>
          </div>
        </div>
        
        <div className="card">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-yellow-500/10 rounded-lg">
              <Clock className="w-5 h-5 text-yellow-400" />
            </div>
            <div>
              <p className="text-sm text-gray-400">Open Tickets</p>
              <p className="text-xl font-bold text-white">
                {servers.reduce((sum, s) => sum + parseInt(s.open_tickets || 0), 0)}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Servers List */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">Connected Servers</h3>
          <p className="card-subtitle">
            {filteredServers.length} of {servers.length} servers
          </p>
        </div>
        
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Guild ID</th>
                <th>Configuration</th>
                <th>Tickets</th>
                <th>Support Roles</th>
                <th>Last Updated</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredServers.map((server) => (
                <tr key={server.guild_id}>
                  <td>
                    <div className="flex items-center gap-2">
                      <div className="w-2 h-2 bg-green-400 rounded-full"></div>
                      <code className="text-sm bg-slate-800 px-2 py-1 rounded">
                        {server.guild_id}
                      </code>
                    </div>
                  </td>
                  <td>
                    <div className="flex items-center gap-2">
                      {server.category_id && server.panel_channel_id && server.transcript_channel_id ? (
                        <>
                          <CheckCircle className="w-4 h-4 text-green-400" />
                          <span className="text-green-400">Complete</span>
                        </>
                      ) : (
                        <>
                          <XCircle className="w-4 h-4 text-red-400" />
                          <span className="text-red-400">Incomplete</span>
                        </>
                      )}
                    </div>
                  </td>
                  <td>
                    <div className="text-sm">
                      <div className="text-white font-medium">
                        {server.total_tickets || 0} total
                      </div>
                      <div className="text-gray-400">
                        {server.open_tickets || 0} open
                      </div>
                    </div>
                  </td>
                  <td>
                    <span className="text-sm text-gray-300">
                      {server.support_roles_count || 0} roles
                    </span>
                  </td>
                  <td>
                    <span className="text-sm text-gray-400">
                      {format(new Date(server.updated_at), 'MMM dd, yyyy')}
                    </span>
                  </td>
                  <td>
                    <button
                      onClick={() => fetchServerDetails(server.guild_id)}
                      className="btn btn-sm btn-secondary"
                    >
                      <Eye className="w-4 h-4" />
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          
          {filteredServers.length === 0 && (
            <div className="text-center py-8 text-gray-400">
              <Server className="w-8 h-8 mx-auto mb-2 opacity-50" />
              <p>No servers found</p>
            </div>
          )}
        </div>
      </div>

      {/* Server Details Modal */}
      {selectedServer && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-slate-800 rounded-lg max-w-2xl w-full max-h-[80vh] overflow-y-auto">
            <div className="p-6 border-b border-slate-700">
              <div className="flex items-center justify-between">
                <h3 className="text-xl font-bold text-white">Server Details</h3>
                <button
                  onClick={() => setSelectedServer(null)}
                  className="text-gray-400 hover:text-white"
                >
                  <XCircle className="w-6 h-6" />
                </button>
              </div>
            </div>
            
            <div className="p-6 space-y-6">
              {/* Basic Info */}
              <div>
                <h4 className="text-lg font-semibold text-white mb-3">Basic Information</h4>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-gray-400">Guild ID</p>
                    <code className="text-sm bg-slate-900 px-2 py-1 rounded">
                      {selectedServer.config.guild_id}
                    </code>
                  </div>
                  <div>
                    <p className="text-sm text-gray-400">Ticket Counter</p>
                    <p className="text-white font-medium">{selectedServer.config.ticket_counter}</p>
                  </div>
                </div>
              </div>

              {/* Configuration */}
              <div>
                <h4 className="text-lg font-semibold text-white mb-3">Configuration</h4>
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 bg-slate-900 rounded">
                    <span className="text-gray-300">Category ID</span>
                    <code className="text-sm">{selectedServer.config.category_id || 'Not set'}</code>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-slate-900 rounded">
                    <span className="text-gray-300">Panel Channel</span>
                    <code className="text-sm">{selectedServer.config.panel_channel_id || 'Not set'}</code>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-slate-900 rounded">
                    <span className="text-gray-300">Transcript Channel</span>
                    <code className="text-sm">{selectedServer.config.transcript_channel_id || 'Not set'}</code>
                  </div>
                  <div className="flex items-center justify-between p-3 bg-slate-900 rounded">
                    <span className="text-gray-300">Error Log Channel</span>
                    <code className="text-sm">{selectedServer.config.error_log_channel_id || 'Not set'}</code>
                  </div>
                </div>
              </div>

              {/* Support Roles */}
              <div>
                <h4 className="text-lg font-semibold text-white mb-3">Support Roles</h4>
                <div className="space-y-2">
                  {selectedServer.supportRoles.map((roleId, index) => (
                    <div key={index} className="p-2 bg-slate-900 rounded">
                      <code className="text-sm">{roleId}</code>
                    </div>
                  ))}
                  {selectedServer.supportRoles.length === 0 && (
                    <p className="text-gray-400 text-sm">No support roles configured</p>
                  )}
                </div>
              </div>

              {/* Statistics */}
              <div>
                <h4 className="text-lg font-semibold text-white mb-3">Statistics</h4>
                <div className="grid grid-cols-2 gap-4">
                  <div className="p-3 bg-slate-900 rounded text-center">
                    <p className="text-2xl font-bold text-blue-400">{selectedServer.stats.total_tickets}</p>
                    <p className="text-sm text-gray-400">Total Tickets</p>
                  </div>
                  <div className="p-3 bg-slate-900 rounded text-center">
                    <p className="text-2xl font-bold text-yellow-400">{selectedServer.stats.open_tickets}</p>
                    <p className="text-sm text-gray-400">Open Tickets</p>
                  </div>
                  <div className="p-3 bg-slate-900 rounded text-center">
                    <p className="text-2xl font-bold text-green-400">{selectedServer.stats.closed_tickets}</p>
                    <p className="text-sm text-gray-400">Closed Tickets</p>
                  </div>
                  <div className="p-3 bg-slate-900 rounded text-center">
                    <p className="text-2xl font-bold text-red-400">{selectedServer.stats.deleted_tickets}</p>
                    <p className="text-sm text-gray-400">Deleted Tickets</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Servers;