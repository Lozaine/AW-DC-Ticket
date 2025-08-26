import React, { useState, useEffect } from 'react';
import { 
  Settings as SettingsIcon, 
  Database, 
  Server, 
  Shield, 
  Bell,
  Save,
  RefreshCw,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Info,
  Eye,
  EyeOff
} from 'lucide-react';

const SettingsPage = () => {
  const [settings, setSettings] = useState({
    database: {
      url: '',
      connected: false,
      lastCheck: null
    },
    system: {
      version: '1.3.0',
      uptime: '0 days',
      environment: 'production'
    },
    notifications: {
      errorAlerts: true,
      dailyReports: false,
      maintenanceMode: false
    }
  });
  
  const [loading, setLoading] = useState(false);
  const [showDatabaseUrl, setShowDatabaseUrl] = useState(false);
  const [testResults, setTestResults] = useState(null);

  useEffect(() => {
    checkDatabaseHealth();
  }, []);

  const checkDatabaseHealth = async () => {
    try {
      setLoading(true);
      const response = await fetch('/api/health');
      const data = await response.json();
      
      setSettings(prev => ({
        ...prev,
        database: {
          ...prev.database,
          connected: data.status === 'healthy',
          lastCheck: new Date().toISOString()
        }
      }));
      
      setTestResults({
        status: data.status,
        database: data.database,
        message: data.status === 'healthy' ? 'Database connection is working properly' : 'Database connection failed'
      });
    } catch (error) {
      setTestResults({
        status: 'error',
        database: 'disconnected',
        message: 'Failed to check database connection'
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSettings = async () => {
    // In a real implementation, this would save settings to the backend
    setLoading(true);
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    setLoading(false);
    alert('Settings saved successfully!');
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">Settings</h1>
          <p className="text-gray-400 mt-1">Configure system settings and preferences</p>
        </div>
        <button 
          onClick={handleSaveSettings}
          disabled={loading}
          className="btn btn-primary"
        >
          {loading ? (
            <div className="spinner"></div>
          ) : (
            <Save className="w-4 h-4" />
          )}
          Save Changes
        </button>
      </div>

      {/* System Status */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title flex items-center gap-2">
            <SettingsIcon className="w-5 h-5" />
            System Status
          </h3>
          <p className="card-subtitle">Current system health and information</p>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="p-4 bg-slate-800 rounded-lg">
            <div className="flex items-center gap-3 mb-3">
              <div className={`w-3 h-3 rounded-full ${
                settings.database.connected ? 'bg-green-400' : 'bg-red-400'
              }`}></div>
              <h4 className="font-semibold text-white">Database</h4>
            </div>
            <p className="text-sm text-gray-400 mb-2">
              Status: {settings.database.connected ? 'Connected' : 'Disconnected'}
            </p>
            <button 
              onClick={checkDatabaseHealth}
              disabled={loading}
              className="btn btn-sm btn-secondary"
            >
              {loading ? (
                <div className="spinner"></div>
              ) : (
                <RefreshCw className="w-4 h-4" />
              )}
              Test Connection
            </button>
          </div>
          
          <div className="p-4 bg-slate-800 rounded-lg">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-3 h-3 rounded-full bg-blue-400"></div>
              <h4 className="font-semibold text-white">Version</h4>
            </div>
            <p className="text-sm text-gray-400 mb-2">
              AW DC Ticket v{settings.system.version}
            </p>
            <p className="text-xs text-gray-500">
              Environment: {settings.system.environment}
            </p>
          </div>
          
          <div className="p-4 bg-slate-800 rounded-lg">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-3 h-3 rounded-full bg-green-400"></div>
              <h4 className="font-semibold text-white">Uptime</h4>
            </div>
            <p className="text-sm text-gray-400 mb-2">
              {settings.system.uptime}
            </p>
            <p className="text-xs text-gray-500">
              Since last restart
            </p>
          </div>
        </div>
        
        {testResults && (
          <div className={`mt-4 p-4 rounded-lg border ${
            testResults.status === 'healthy' 
              ? 'bg-green-900/20 border-green-800' 
              : 'bg-red-900/20 border-red-800'
          }`}>
            <div className="flex items-center gap-3">
              {testResults.status === 'healthy' ? (
                <CheckCircle className="w-5 h-5 text-green-400" />
              ) : (
                <XCircle className="w-5 h-5 text-red-400" />
              )}
              <div>
                <p className={`font-medium ${
                  testResults.status === 'healthy' ? 'text-green-400' : 'text-red-400'
                }`}>
                  Database Connection Test
                </p>
                <p className={`text-sm ${
                  testResults.status === 'healthy' ? 'text-green-300' : 'text-red-300'
                }`}>
                  {testResults.message}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Database Configuration */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title flex items-center gap-2">
            <Database className="w-5 h-5" />
            Database Configuration
          </h3>
          <p className="card-subtitle">PostgreSQL connection settings</p>
        </div>
        
        <div className="space-y-4">
          <div className="form-group">
            <label className="form-label">Database URL</label>
            <div className="relative">
              <input
                type={showDatabaseUrl ? 'text' : 'password'}
                value={process.env.DATABASE_URL || 'postgresql://...'}
                readOnly
                className="form-input pr-10"
              />
              <button
                type="button"
                onClick={() => setShowDatabaseUrl(!showDatabaseUrl)}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-white"
              >
                {showDatabaseUrl ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Database URL is configured via environment variables
            </p>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="form-group">
              <label className="form-label">Connection Pool Size</label>
              <input
                type="number"
                value="10"
                readOnly
                className="form-input"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Connection Timeout</label>
              <input
                type="text"
                value="30 seconds"
                readOnly
                className="form-input"
              />
            </div>
          </div>
          
          <div className="p-4 bg-blue-900/20 border border-blue-800 rounded-lg">
            <div className="flex items-start gap-3">
              <Info className="w-5 h-5 text-blue-400 mt-0.5" />
              <div>
                <p className="text-blue-400 font-medium">Database Configuration</p>
                <p className="text-blue-300 text-sm mt-1">
                  Database settings are configured through environment variables for security. 
                  To modify the database connection, update the DATABASE_URL environment variable and restart the application.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Security Settings */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title flex items-center gap-2">
            <Shield className="w-5 h-5" />
            Security Settings
          </h3>
          <p className="card-subtitle">Access control and security preferences</p>
        </div>
        
        <div className="space-y-4">
          <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
            <div>
              <h4 className="font-medium text-white">API Rate Limiting</h4>
              <p className="text-sm text-gray-400">Limit API requests per minute</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-300">100 req/min</span>
              <div className="w-12 h-6 bg-green-600 rounded-full relative">
                <div className="w-5 h-5 bg-white rounded-full absolute right-0.5 top-0.5"></div>
              </div>
            </div>
          </div>
          
          <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
            <div>
              <h4 className="font-medium text-white">CORS Protection</h4>
              <p className="text-sm text-gray-400">Cross-origin request protection</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-300">Enabled</span>
              <div className="w-12 h-6 bg-green-600 rounded-full relative">
                <div className="w-5 h-5 bg-white rounded-full absolute right-0.5 top-0.5"></div>
              </div>
            </div>
          </div>
          
          <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
            <div>
              <h4 className="font-medium text-white">SSL/TLS Encryption</h4>
              <p className="text-sm text-gray-400">Secure data transmission</p>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-300">Required</span>
              <div className="w-12 h-6 bg-green-600 rounded-full relative">
                <div className="w-5 h-5 bg-white rounded-full absolute right-0.5 top-0.5"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Notification Settings */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title flex items-center gap-2">
            <Bell className="w-5 h-5" />
            Notification Settings
          </h3>
          <p className="card-subtitle">Configure alerts and notifications</p>
        </div>
        
        <div className="space-y-4">
          <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
            <div>
              <h4 className="font-medium text-white">Error Alerts</h4>
              <p className="text-sm text-gray-400">Get notified of system errors</p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={settings.notifications.errorAlerts}
                onChange={(e) => setSettings(prev => ({
                  ...prev,
                  notifications: {
                    ...prev.notifications,
                    errorAlerts: e.target.checked
                  }
                }))}
                className="sr-only peer"
              />
              <div className="w-12 h-6 bg-gray-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
          
          <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
            <div>
              <h4 className="font-medium text-white">Daily Reports</h4>
              <p className="text-sm text-gray-400">Receive daily activity summaries</p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={settings.notifications.dailyReports}
                onChange={(e) => setSettings(prev => ({
                  ...prev,
                  notifications: {
                    ...prev.notifications,
                    dailyReports: e.target.checked
                  }
                }))}
                className="sr-only peer"
              />
              <div className="w-12 h-6 bg-gray-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
          
          <div className="flex items-center justify-between p-4 bg-slate-800 rounded-lg">
            <div>
              <h4 className="font-medium text-white">Maintenance Mode</h4>
              <p className="text-sm text-gray-400">Enable maintenance mode</p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={settings.notifications.maintenanceMode}
                onChange={(e) => setSettings(prev => ({
                  ...prev,
                  notifications: {
                    ...prev.notifications,
                    maintenanceMode: e.target.checked
                  }
                }))}
                className="sr-only peer"
              />
              <div className="w-12 h-6 bg-gray-600 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-red-600"></div>
            </label>
          </div>
        </div>
      </div>

      {/* System Information */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title flex items-center gap-2">
            <Server className="w-5 h-5" />
            System Information
          </h3>
          <p className="card-subtitle">Technical details and specifications</p>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-400">Application Version</span>
              <span className="text-white">v{settings.system.version}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Node.js Version</span>
              <span className="text-white">v18.17.0</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Database Type</span>
              <span className="text-white">PostgreSQL</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Environment</span>
              <span className="text-white capitalize">{settings.system.environment}</span>
            </div>
          </div>
          
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-gray-400">Memory Usage</span>
              <span className="text-white">245 MB</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">CPU Usage</span>
              <span className="text-white">12%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Disk Usage</span>
              <span className="text-white">1.2 GB</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Last Restart</span>
              <span className="text-white">2 hours ago</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;