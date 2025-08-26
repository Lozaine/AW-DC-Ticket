import React, { useState, useEffect } from 'react';
import { 
  Ticket, 
  Search, 
  Filter, 
  Download, 
  RefreshCw,
  Eye,
  Clock,
  CheckCircle,
  XCircle,
  Trash2,
  RotateCcw,
  ChevronLeft,
  ChevronRight,
  Calendar,
  User,
  Server
} from 'lucide-react';
import { format } from 'date-fns';

const Tickets = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [guildFilter, setGuildFilter] = useState('');
  const [pagination, setPagination] = useState({
    page: 1,
    limit: 50,
    total: 0,
    totalPages: 0
  });

  useEffect(() => {
    fetchTickets();
  }, [pagination.page, statusFilter, guildFilter]);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams({
        page: pagination.page.toString(),
        limit: pagination.limit.toString(),
        ...(statusFilter && { status: statusFilter }),
        ...(guildFilter && { guild_id: guildFilter })
      });

      const response = await fetch(`/api/tickets?${params}`);
      if (!response.ok) throw new Error('Failed to fetch tickets');
      
      const data = await response.json();
      setTickets(data.tickets);
      setPagination(prev => ({
        ...prev,
        total: data.pagination.total,
        totalPages: data.pagination.totalPages
      }));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handlePageChange = (newPage) => {
    setPagination(prev => ({ ...prev, page: newPage }));
  };

  const handleStatusFilter = (status) => {
    setStatusFilter(status);
    setPagination(prev => ({ ...prev, page: 1 }));
  };

  const exportTickets = async () => {
    try {
      const params = new URLSearchParams({
        ...(statusFilter && { status: statusFilter }),
        ...(guildFilter && { guild_id: guildFilter }),
        limit: '10000' // Export all matching tickets
      });

      const response = await fetch(`/api/tickets?${params}`);
      const data = await response.json();
      
      // Convert to CSV
      const headers = ['ID', 'Guild ID', 'Channel Name', 'Owner ID', 'Type', 'Number', 'Status', 'Created', 'Closed', 'Closed By'];
      const csvContent = [
        headers.join(','),
        ...data.tickets.map(ticket => [
          ticket.id,
          ticket.guild_id,
          `"${ticket.channel_name}"`,
          ticket.owner_id,
          ticket.ticket_type,
          ticket.ticket_number,
          ticket.status,
          ticket.created_at,
          ticket.closed_at || '',
          `"${ticket.closed_by || ''}"`
        ].join(','))
      ].join('\n');

      // Download file
      const blob = new Blob([csvContent], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `tickets-${format(new Date(), 'yyyy-MM-dd')}.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Export failed:', err);
    }
  };

  const filteredTickets = tickets.filter(ticket =>
    ticket.channel_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    ticket.owner_id.includes(searchTerm) ||
    ticket.guild_id.includes(searchTerm)
  );

  const getStatusIcon = (status) => {
    switch (status) {
      case 'open': return <Clock className="w-4 h-4 text-yellow-400" />;
      case 'closed': return <CheckCircle className="w-4 h-4 text-green-400" />;
      case 'deleted': return <Trash2 className="w-4 h-4 text-red-400" />;
      case 'auto_closed': return <Clock className="w-4 h-4 text-orange-400" />;
      case 'reopened': return <RotateCcw className="w-4 h-4 text-purple-400" />;
      default: return <Ticket className="w-4 h-4 text-gray-400" />;
    }
  };

  const getTypeColor = (type) => {
    switch (type) {
      case 'Support': return 'text-blue-400';
      case 'Report': return 'text-red-400';
      case 'Appeal': return 'text-purple-400';
      default: return 'text-gray-400';
    }
  };

  if (loading && tickets.length === 0) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold text-white">Tickets</h1>
          <div className="spinner"></div>
        </div>
        <div className="card">
          <div className="animate-pulse space-y-4">
            {[...Array(10)].map((_, i) => (
              <div key={i} className="h-16 bg-slate-700 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Tickets</h1>
          <p className="text-gray-400 mt-1">View and manage all support tickets</p>
        </div>
        <div className="flex gap-2">
          <button onClick={exportTickets} className="btn btn-secondary">
            <Download className="w-4 h-4" />
            Export CSV
          </button>
          <button onClick={fetchTickets} className="btn btn-primary">
            <RefreshCw className="w-4 h-4" />
            Refresh
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="relative">
          <Search className="w-5 h-5 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Search tickets..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="form-input pl-10"
          />
        </div>
        
        <select
          value={statusFilter}
          onChange={(e) => handleStatusFilter(e.target.value)}
          className="form-input"
        >
          <option value="">All Statuses</option>
          <option value="open">Open</option>
          <option value="closed">Closed</option>
          <option value="deleted">Deleted</option>
          <option value="auto_closed">Auto Closed</option>
          <option value="reopened">Reopened</option>
        </select>
        
        <input
          type="text"
          placeholder="Filter by Guild ID..."
          value={guildFilter}
          onChange={(e) => setGuildFilter(e.target.value)}
          className="form-input"
        />
        
        <div className="flex items-center gap-2 text-sm text-gray-400">
          <Ticket className="w-4 h-4" />
          {pagination.total.toLocaleString()} total tickets
        </div>
      </div>

      {/* Quick Status Filters */}
      <div className="flex gap-2 flex-wrap">
        {[
          { label: 'All', value: '', count: pagination.total },
          { label: 'Open', value: 'open', color: 'text-yellow-400' },
          { label: 'Closed', value: 'closed', color: 'text-green-400' },
          { label: 'Deleted', value: 'deleted', color: 'text-red-400' }
        ].map((filter) => (
          <button
            key={filter.value}
            onClick={() => handleStatusFilter(filter.value)}
            className={`px-3 py-1 rounded-full text-sm transition-colors ${
              statusFilter === filter.value
                ? 'bg-blue-600 text-white'
                : 'bg-slate-700 text-gray-300 hover:bg-slate-600'
            }`}
          >
            {filter.label}
            {filter.count && (
              <span className="ml-1 text-xs opacity-75">({filter.count})</span>
            )}
          </button>
        ))}
      </div>

      {/* Tickets Table */}
      <div className="card">
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Ticket</th>
                <th>Type</th>
                <th>Status</th>
                <th>Owner</th>
                <th>Guild</th>
                <th>Created</th>
                <th>Closed</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredTickets.map((ticket) => (
                <tr key={ticket.id}>
                  <td>
                    <div>
                      <div className="font-medium text-white">
                        {ticket.channel_name}
                      </div>
                      <div className="text-sm text-gray-400">
                        #{ticket.ticket_number || 'N/A'}
                      </div>
                    </div>
                  </td>
                  <td>
                    <span className={`font-medium ${getTypeColor(ticket.ticket_type)}`}>
                      {ticket.ticket_type}
                    </span>
                  </td>
                  <td>
                    <div className="flex items-center gap-2">
                      {getStatusIcon(ticket.status)}
                      <span className={`status-badge status-${ticket.status}`}>
                        {ticket.status}
                      </span>
                    </div>
                  </td>
                  <td>
                    <div className="flex items-center gap-2">
                      <User className="w-4 h-4 text-gray-400" />
                      <code className="text-sm">{ticket.owner_id}</code>
                    </div>
                  </td>
                  <td>
                    <div className="flex items-center gap-2">
                      <Server className="w-4 h-4 text-gray-400" />
                      <code className="text-sm">{ticket.guild_id}</code>
                    </div>
                  </td>
                  <td>
                    <div className="text-sm">
                      <div className="text-white">
                        {format(new Date(ticket.created_at), 'MMM dd, yyyy')}
                      </div>
                      <div className="text-gray-400">
                        {format(new Date(ticket.created_at), 'HH:mm')}
                      </div>
                    </div>
                  </td>
                  <td>
                    {ticket.closed_at ? (
                      <div className="text-sm">
                        <div className="text-white">
                          {format(new Date(ticket.closed_at), 'MMM dd, yyyy')}
                        </div>
                        <div className="text-gray-400">
                          {format(new Date(ticket.closed_at), 'HH:mm')}
                        </div>
                      </div>
                    ) : (
                      <span className="text-gray-400">-</span>
                    )}
                  </td>
                  <td>
                    <button className="btn btn-sm btn-secondary">
                      <Eye className="w-4 h-4" />
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          
          {filteredTickets.length === 0 && !loading && (
            <div className="text-center py-12 text-gray-400">
              <Ticket className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <h3 className="text-lg font-medium mb-2">No tickets found</h3>
              <p>Try adjusting your search or filter criteria</p>
            </div>
          )}
        </div>
      </div>

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-gray-400">
            Showing {((pagination.page - 1) * pagination.limit) + 1} to{' '}
            {Math.min(pagination.page * pagination.limit, pagination.total)} of{' '}
            {pagination.total.toLocaleString()} tickets
          </div>
          
          <div className="flex items-center gap-2">
            <button
              onClick={() => handlePageChange(pagination.page - 1)}
              disabled={pagination.page === 1}
              className="btn btn-sm btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="w-4 h-4" />
              Previous
            </button>
            
            <div className="flex items-center gap-1">
              {[...Array(Math.min(5, pagination.totalPages))].map((_, i) => {
                const pageNum = i + 1;
                return (
                  <button
                    key={pageNum}
                    onClick={() => handlePageChange(pageNum)}
                    className={`w-8 h-8 rounded text-sm ${
                      pagination.page === pageNum
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-700 text-gray-300 hover:bg-slate-600'
                    }`}
                  >
                    {pageNum}
                  </button>
                );
              })}
              
              {pagination.totalPages > 5 && (
                <>
                  <span className="text-gray-400">...</span>
                  <button
                    onClick={() => handlePageChange(pagination.totalPages)}
                    className={`w-8 h-8 rounded text-sm ${
                      pagination.page === pagination.totalPages
                        ? 'bg-blue-600 text-white'
                        : 'bg-slate-700 text-gray-300 hover:bg-slate-600'
                    }`}
                  >
                    {pagination.totalPages}
                  </button>
                </>
              )}
            </div>
            
            <button
              onClick={() => handlePageChange(pagination.page + 1)}
              disabled={pagination.page === pagination.totalPages}
              className="btn btn-sm btn-secondary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {error && (
        <div className="card bg-red-900/20 border-red-800">
          <div className="flex items-center gap-3">
            <XCircle className="w-5 h-5 text-red-400" />
            <div>
              <p className="text-red-400 font-medium">Error loading tickets</p>
              <p className="text-red-300 text-sm">{error}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Tickets;