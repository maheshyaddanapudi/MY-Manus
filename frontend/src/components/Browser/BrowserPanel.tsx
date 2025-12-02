import React, { useEffect, useState } from 'react';
import { apiService } from '../../services/api';
import { SnapshotViewer } from './SnapshotViewer';

interface BrowserSnapshot {
  id: string;
  timestamp: number;
  screenshot: string;
  url: string;
  title: string;
  htmlContent?: string;
  accessibilityTree?: string;
}

interface BrowserPanelProps {
  sessionId: string;
}

/**
 * Browser Panel - Displays browser snapshots from tool executions
 * Shows historical browser states captured by browser_view tool
 */
export const BrowserPanel: React.FC<BrowserPanelProps> = ({ sessionId }) => {
  const [snapshots, setSnapshots] = useState<BrowserSnapshot[]>([]);
  const [selectedSnapshot, setSelectedSnapshot] = useState<BrowserSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'screenshot' | 'html' | 'tree'>('screenshot');

  // Fetch tool executions and extract browser snapshots
  const fetchSnapshots = async () => {
    if (!sessionId) return;

    try {
      setLoading(true);
      const toolExecutions = await apiService.getToolExecutions(sessionId);

      // Filter browser_view executions and extract snapshots
      const browserSnapshots: BrowserSnapshot[] = toolExecutions
        .filter((exec: any) => exec.toolName === 'browser_view' && exec.result?.screenshot)
        .map((exec: any) => ({
          id: exec.id.toString(),
          timestamp: exec.result.timestamp || new Date(exec.timestamp).getTime(),
          screenshot: exec.result.screenshot,
          url: exec.result.url || '',
          title: exec.result.title || 'Untitled',
          htmlContent: exec.result.htmlContent,
          accessibilityTree: exec.result.accessibilityTree,
        }))
        .sort((a, b) => b.timestamp - a.timestamp); // Most recent first

      setSnapshots(browserSnapshots);

      // Auto-select most recent snapshot
      if (browserSnapshots.length > 0 && !selectedSnapshot) {
        setSelectedSnapshot(browserSnapshots[0]);
      }

      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch browser snapshots');
      console.error('Error fetching snapshots:', err);
    } finally {
      setLoading(false);
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchSnapshots();
  }, [sessionId]);

  // Auto-refresh every 3 seconds
  useEffect(() => {
    const interval = setInterval(fetchSnapshots, 3000);
    return () => clearInterval(interval);
  }, [sessionId]);

  return (
    <div className="flex flex-col h-full bg-gradient-to-b from-gray-900 to-gray-900/95 text-gray-100">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-700/50">
        <div className="flex items-center space-x-3">
          <h2 className="text-lg font-semibold">🌐 Browser</h2>
          <span className="px-2 py-1 text-xs bg-blue-600 rounded">
            {snapshots.length} snapshot{snapshots.length !== 1 ? 's' : ''}
          </span>
        </div>

        {/* View Mode Selector */}
        {selectedSnapshot && (
          <div className="flex space-x-2">
            <button
              onClick={() => setViewMode('screenshot')}
              className={`px-3 py-1 text-sm rounded transition-colors ${
                viewMode === 'screenshot'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              Screenshot
            </button>
            <button
              onClick={() => setViewMode('html')}
              className={`px-3 py-1 text-sm rounded transition-colors ${
                viewMode === 'html'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              HTML
            </button>
            <button
              onClick={() => setViewMode('tree')}
              className={`px-3 py-1 text-sm rounded transition-colors ${
                viewMode === 'tree'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
              }`}
            >
              A11y Tree
            </button>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Snapshot List (Sidebar) */}
        {snapshots.length > 1 && (
          <div className="w-48 border-r border-gray-700/50 overflow-y-auto bg-gray-800/40">
            <div className="p-2">
              <div className="text-xs text-gray-400 mb-2 px-2">History</div>
              {snapshots.map((snapshot) => (
                <button
                  key={snapshot.id}
                  onClick={() => setSelectedSnapshot(snapshot)}
                  className={`w-full text-left p-2 rounded mb-1 transition-colors ${
                    selectedSnapshot?.id === snapshot.id
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  }`}
                >
                  <div className="text-xs truncate">{snapshot.title}</div>
                  <div className="text-xs text-gray-400 truncate">
                    {new Date(snapshot.timestamp).toLocaleTimeString()}
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Snapshot Viewer */}
        <div className="flex-1 overflow-hidden">
          {loading && snapshots.length === 0 ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-gray-500">Loading browser snapshots...</div>
            </div>
          ) : error ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-red-500">{error}</div>
            </div>
          ) : snapshots.length === 0 ? (
            <div className="flex items-center justify-center h-full">
              <div className="text-center">
                <div className="text-gray-500 mb-2">No browser snapshots yet</div>
                <div className="text-xs text-gray-600">
                  Browser snapshots will appear here when the agent uses browser tools
                </div>
              </div>
            </div>
          ) : selectedSnapshot ? (
            <SnapshotViewer snapshot={selectedSnapshot} viewMode={viewMode} />
          ) : null}
        </div>
      </div>
    </div>
  );
};
