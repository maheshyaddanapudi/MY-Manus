import React, { useState, useEffect } from 'react';

interface ConsoleLog {
  id: number;
  timestamp: number;
  level: 'log' | 'warn' | 'error' | 'info' | 'debug';
  message: string;
  source?: string;
  lineNumber?: number;
}

interface ConsoleViewerProps {
  sessionId: string;
}

export const ConsoleViewer: React.FC<ConsoleViewerProps> = ({ sessionId }) => {
  const [logs, setLogs] = useState<ConsoleLog[]>([]);
  const [filter, setFilter] = useState<'all' | 'log' | 'warn' | 'error' | 'info'>('all');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchConsoleLogs();
    const interval = setInterval(fetchConsoleLogs, 2000);
    return () => clearInterval(interval);
  }, [sessionId]);

  const fetchConsoleLogs = async () => {
    try {
      const response = await fetch(`/api/browser/${sessionId}/console-logs`);
      if (response.ok) {
        const data = await response.json();
        setLogs(data);
      }
    } catch (error) {
      console.error('Failed to fetch console logs:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredLogs = filter === 'all'
    ? logs
    : logs.filter(log => log.level === filter);

  const getLevelColor = (level: ConsoleLog['level']) => {
    switch (level) {
      case 'error': return 'text-red-400';
      case 'warn': return 'text-yellow-400';
      case 'info': return 'text-blue-400';
      case 'debug': return 'text-gray-400';
      default: return 'text-gray-300';
    }
  };

  const getLevelIcon = (level: ConsoleLog['level']) => {
    switch (level) {
      case 'error': return '❌';
      case 'warn': return '⚠️';
      case 'info': return 'ℹ️';
      case 'debug': return '🔍';
      default: return '📝';
    }
  };

  const clearLogs = async () => {
    try {
      await fetch(`/api/browser/${sessionId}/console-logs`, { method: 'DELETE' });
      setLogs([]);
    } catch (error) {
      console.error('Failed to clear console logs:', error);
    }
  };

  return (
    <div className="h-full flex flex-col bg-gray-900">
      {/* Header */}
      <div className="border-b border-gray-700 px-4 py-2 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <span className="text-sm font-semibold">Console</span>
          <span className="text-xs text-gray-400">({filteredLogs.length} logs)</span>
        </div>
        <div className="flex items-center space-x-2">
          {/* Filter */}
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value as any)}
            className="text-xs bg-gray-800 border border-gray-600 rounded px-2 py-1"
          >
            <option value="all">All</option>
            <option value="log">Log</option>
            <option value="info">Info</option>
            <option value="warn">Warn</option>
            <option value="error">Error</option>
          </select>
          <button
            onClick={clearLogs}
            className="text-xs px-2 py-1 bg-gray-700 hover:bg-gray-600 rounded"
          >
            🗑️ Clear
          </button>
        </div>
      </div>

      {/* Logs */}
      <div className="flex-1 overflow-y-auto p-2 font-mono text-xs">
        {loading && (
          <div className="text-gray-400 text-center py-4">Loading console logs...</div>
        )}

        {!loading && logs.length === 0 && (
          <div className="text-gray-400 text-center py-4">
            No console logs yet. Logs will appear here when browser tools are executed.
          </div>
        )}

        {!loading && filteredLogs.length === 0 && logs.length > 0 && (
          <div className="text-gray-400 text-center py-4">
            No logs match the current filter.
          </div>
        )}

        {filteredLogs.map((log) => (
          <div
            key={log.id}
            className="border-b border-gray-800 py-2 px-2 hover:bg-gray-800"
          >
            <div className="flex items-start space-x-2">
              <span className="flex-shrink-0">{getLevelIcon(log.level)}</span>
              <div className="flex-1 min-w-0">
                <div className={`${getLevelColor(log.level)}`}>
                  {log.message}
                </div>
                {log.source && (
                  <div className="text-gray-500 text-xs mt-1">
                    {log.source}
                    {log.lineNumber && `:${log.lineNumber}`}
                  </div>
                )}
              </div>
              <span className="flex-shrink-0 text-gray-500 text-xs">
                {new Date(log.timestamp).toLocaleTimeString()}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
