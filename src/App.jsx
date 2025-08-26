import React, { useState, useEffect } from 'react';
import { 
  BarChart3, 
  Server, 
  Ticket, 
  Users, 
  Activity, 
  Settings, 
  Database,
  TrendingUp,
  Clock,
  CheckCircle,
  XCircle,
  Trash2,
  RotateCcw,
  Calendar,
  Search,
  Filter,
  Download,
  RefreshCw,
  AlertCircle,
  Eye
} from 'lucide-react';
import Dashboard from './components/Dashboard';
import Servers from './components/Servers';
import Tickets from './components/Tickets';
import Analytics from './components/Analytics';
import SettingsPage from './components/Settings';

const App = () => {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [isLoading, setIsLoading] = useState(true);
  const [healthStatus, setHealthStatus] = useState(null);

  useEffect(() => {
    checkHealth();
  }, []);

  const checkHealth = async () => {
    try {
      const response = await fetch('/api/health');
      const data = await response.json();
      setHealthStatus(data);
    } catch (error) {
      setHealthStatus({ status: 'unhealthy', database: 'disconnected' });
    } finally {
      setIsLoading(false);
    }
  };

  const navigation = [
    { id: 'dashboard', name: 'Dashboard', icon: BarChart3 },
    { id: 'servers', name: 'Servers', icon: Server },
    { id: 'tickets', name: 'Tickets', icon: Ticket },
    { id: 'analytics', name: 'Analytics', icon: TrendingUp },
    { id: 'settings', name: 'Settings', icon: Settings },
  ];

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <Dashboard />;
      case 'servers':
        return <Servers />;
      case 'tickets':
        return <Tickets />;
      case 'analytics':
        return <Analytics />;
      case 'settings':
        return <SettingsPage />;
      default:
        return <Dashboard />;
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="spinner mx-auto mb-4"></div>
          <p className="text-gray-400">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-900">
      {/* Header */}
      <header className="bg-slate-800 border-b border-slate-700">
        <div className="px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
                  <Ticket className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h1 className="text-xl font-bold text-white">AW DC Ticket</h1>
                  <p className="text-sm text-gray-400">Dashboard v1.3.0</p>
                </div>
              </div>
            </div>
            
            <div className="flex items-center gap-4">
              {/* Health Status */}
              <div className="flex items-center gap-2">
                <div className={`w-2 h-2 rounded-full ${
                  healthStatus?.status === 'healthy' ? 'bg-green-400' : 'bg-red-400'
                }`}></div>
                <span className="text-sm text-gray-400">
                  {healthStatus?.status === 'healthy' ? 'System Healthy' : 'System Issues'}
                </span>
              </div>
              
              <button
                onClick={checkHealth}
                className="p-2 text-gray-400 hover:text-white transition-colors"
                title="Refresh health status"
              >
                <RefreshCw className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="flex">
        {/* Sidebar */}
        <nav className="w-64 bg-slate-800 border-r border-slate-700 min-h-screen">
          <div className="p-6">
            <ul className="space-y-2">
              {navigation.map((item) => {
                const Icon = item.icon;
                return (
                  <li key={item.id}>
                    <button
                      onClick={() => setActiveTab(item.id)}
                      className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                        activeTab === item.id
                          ? 'bg-blue-600 text-white'
                          : 'text-gray-300 hover:bg-slate-700 hover:text-white'
                      }`}
                    >
                      <Icon className="w-5 h-5" />
                      {item.name}
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        </nav>

        {/* Main Content */}
        <main className="flex-1 p-6">
          {healthStatus?.status !== 'healthy' && (
            <div className="mb-6 p-4 bg-red-900/20 border border-red-800 rounded-lg flex items-center gap-3">
              <AlertCircle className="w-5 h-5 text-red-400" />
              <div>
                <p className="text-red-400 font-medium">System Health Warning</p>
                <p className="text-red-300 text-sm">
                  Database: {healthStatus?.database || 'Unknown'} - Some features may not work properly
                </p>
              </div>
            </div>
          )}
          
          <div className="fade-in">
            {renderContent()}
          </div>
        </main>
      </div>
    </div>
  );
};

export default App;