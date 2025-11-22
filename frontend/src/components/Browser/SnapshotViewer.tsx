import React from 'react';

interface Snapshot {
  screenshot: string;
  url: string;
  title: string;
  timestamp: number;
  htmlContent?: string;
  accessibilityTree?: string;
}

interface SnapshotViewerProps {
  snapshot: Snapshot;
  viewMode: 'screenshot' | 'html' | 'tree';
}

/**
 * Snapshot Viewer - Displays a browser snapshot in different modes
 */
export const SnapshotViewer: React.FC<SnapshotViewerProps> = ({ snapshot, viewMode }) => {
  return (
    <div className="flex flex-col h-full">
      {/* Snapshot Info */}
      <div className="px-4 py-2 border-b border-gray-700 bg-gray-800">
        <div className="flex items-center justify-between">
          <div className="flex-1 min-w-0 mr-4">
            <div className="text-sm font-medium truncate">{snapshot.title}</div>
            <div className="text-xs text-gray-400 truncate">{snapshot.url}</div>
          </div>
          <div className="text-xs text-gray-500">
            {new Date(snapshot.timestamp).toLocaleString()}
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto bg-gray-900 p-4">
        {viewMode === 'screenshot' && (
          <div className="flex justify-center">
            <img
              src={`data:image/png;base64,${snapshot.screenshot}`}
              alt={snapshot.title}
              className="max-w-full h-auto border border-gray-700 rounded shadow-lg"
            />
          </div>
        )}

        {viewMode === 'html' && (
          <div className="bg-gray-800 rounded p-4">
            {snapshot.htmlContent ? (
              <pre className="text-xs text-gray-300 whitespace-pre-wrap font-mono overflow-x-auto">
                {snapshot.htmlContent}
              </pre>
            ) : (
              <div className="text-gray-500 text-center py-8">
                HTML content not available for this snapshot
              </div>
            )}
          </div>
        )}

        {viewMode === 'tree' && (
          <div className="bg-gray-800 rounded p-4">
            {snapshot.accessibilityTree ? (
              <pre className="text-sm text-gray-300 whitespace-pre font-mono">
                {snapshot.accessibilityTree}
              </pre>
            ) : (
              <div className="text-gray-500 text-center py-8">
                Accessibility tree not available for this snapshot
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
