import React, { useState, useEffect } from 'react';

interface NetworkRequest {
  id: number;
  timestamp: number;
  method: string;
  url: string;
  status: number;
  statusText: string;
  type: string;
  size: number;
  duration: number;
  requestHeaders?: Record<string, string>;
  responseHeaders?: Record<string, string>;
  requestBody?: string;
  responseBody?: string;
}

interface NetworkViewerProps {
  sessionId: string;
}

export const NetworkViewer: React.FC<NetworkViewerProps> = ({ sessionId }) => {
  const [requests, setRequests] = useState<NetworkRequest[]>([]);
  const [selectedRequest, setSelectedRequest] = useState<NetworkRequest | null>(null);
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchNetworkRequests();
    const interval = setInterval(fetchNetworkRequests, 2000);
    return () => clearInterval(interval);
  }, [sessionId]);

  const fetchNetworkRequests = async () => {
    try {
      const response = await fetch(`/api/browser/${sessionId}/network-requests`);
      if (response.ok) {
        const data = await response.json();
        setRequests(data);
      }
    } catch (error) {
      console.error('Failed to fetch network requests:', error);
    } finally {
      setLoading(false);
    }
  };

  const filteredRequests = filter
    ? requests.filter(req =>
        req.url.toLowerCase().includes(filter.toLowerCase()) ||
        req.method.toLowerCase().includes(filter.toLowerCase())
      )
    : requests;

  const getStatusColor = (status: number) => {
    if (status >= 200 && status < 300) return 'text-green-400';
    if (status >= 300 && status < 400) return 'text-blue-400';
    if (status >= 400 && status < 500) return 'text-yellow-400';
    if (status >= 500) return 'text-red-400';
    return 'text-gray-400';
  };

  const getMethodColor = (method: string) => {
    switch (method) {
      case 'GET': return 'text-green-400';
      case 'POST': return 'text-blue-400';
      case 'PUT': return 'text-yellow-400';
      case 'DELETE': return 'text-red-400';
      default: return 'text-gray-400';
    }
  };

  const formatSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const clearRequests = async () => {
    try {
      await fetch(`/api/browser/${sessionId}/network-requests`, { method: 'DELETE' });
      setRequests([]);
      setSelectedRequest(null);
    } catch (error) {
      console.error('Failed to clear network requests:', error);
    }
  };

  return (
    <div className="h-full flex bg-gray-900">
      {/* Request List */}
      <div className="w-2/3 border-r border-gray-700 flex flex-col">
        {/* Header */}
        <div className="border-b border-gray-700 px-4 py-2 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <span className="text-sm font-semibold">Network</span>
            <span className="text-xs text-gray-400">({filteredRequests.length} requests)</span>
          </div>
          <div className="flex items-center space-x-2">
            <input
              type="text"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="Filter..."
              className="text-xs bg-gray-800 border border-gray-600 rounded px-2 py-1 w-40"
            />
            <button
              onClick={clearRequests}
              className="text-xs px-2 py-1 bg-gray-700 hover:bg-gray-600 rounded"
            >
              🗑️ Clear
            </button>
          </div>
        </div>

        {/* Request Table */}
        <div className="flex-1 overflow-y-auto">
          {loading && (
            <div className="text-gray-400 text-center py-4">Loading network requests...</div>
          )}

          {!loading && requests.length === 0 && (
            <div className="text-gray-400 text-center py-4">
              No network requests yet. Requests will appear here when browser tools are executed.
            </div>
          )}

          {!loading && filteredRequests.length === 0 && requests.length > 0 && (
            <div className="text-gray-400 text-center py-4">
              No requests match the current filter.
            </div>
          )}

          <table className="w-full text-xs">
            <thead className="bg-gray-800 sticky top-0">
              <tr>
                <th className="text-left px-3 py-2 font-semibold">Method</th>
                <th className="text-left px-3 py-2 font-semibold">URL</th>
                <th className="text-left px-3 py-2 font-semibold">Status</th>
                <th className="text-left px-3 py-2 font-semibold">Type</th>
                <th className="text-right px-3 py-2 font-semibold">Size</th>
                <th className="text-right px-3 py-2 font-semibold">Time</th>
              </tr>
            </thead>
            <tbody>
              {filteredRequests.map((req) => (
                <tr
                  key={req.id}
                  onClick={() => setSelectedRequest(req)}
                  className={`border-b border-gray-800 hover:bg-gray-800 cursor-pointer ${
                    selectedRequest?.id === req.id ? 'bg-gray-800' : ''
                  }`}
                >
                  <td className={`px-3 py-2 font-semibold ${getMethodColor(req.method)}`}>
                    {req.method}
                  </td>
                  <td className="px-3 py-2 text-gray-300 truncate max-w-xs">
                    {req.url}
                  </td>
                  <td className={`px-3 py-2 ${getStatusColor(req.status)}`}>
                    {req.status}
                  </td>
                  <td className="px-3 py-2 text-gray-400">
                    {req.type}
                  </td>
                  <td className="px-3 py-2 text-right text-gray-400">
                    {formatSize(req.size)}
                  </td>
                  <td className="px-3 py-2 text-right text-gray-400">
                    {req.duration}ms
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Request Details */}
      <div className="w-1/3 flex flex-col">
        {selectedRequest ? (
          <>
            <div className="border-b border-gray-700 px-4 py-2">
              <span className="text-sm font-semibold">Request Details</span>
            </div>
            <div className="flex-1 overflow-y-auto p-4 text-xs">
              <div className="space-y-4">
                {/* General */}
                <div>
                  <h3 className="font-semibold mb-2">General</h3>
                  <div className="space-y-1 text-gray-300">
                    <div><span className="text-gray-400">URL:</span> {selectedRequest.url}</div>
                    <div><span className="text-gray-400">Method:</span> {selectedRequest.method}</div>
                    <div><span className="text-gray-400">Status:</span> {selectedRequest.status} {selectedRequest.statusText}</div>
                    <div><span className="text-gray-400">Type:</span> {selectedRequest.type}</div>
                  </div>
                </div>

                {/* Request Headers */}
                {selectedRequest.requestHeaders && (
                  <div>
                    <h3 className="font-semibold mb-2">Request Headers</h3>
                    <div className="bg-gray-800 p-2 rounded">
                      {Object.entries(selectedRequest.requestHeaders).map(([key, value]) => (
                        <div key={key} className="text-gray-300">
                          <span className="text-blue-400">{key}:</span> {value}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Response Headers */}
                {selectedRequest.responseHeaders && (
                  <div>
                    <h3 className="font-semibold mb-2">Response Headers</h3>
                    <div className="bg-gray-800 p-2 rounded">
                      {Object.entries(selectedRequest.responseHeaders).map(([key, value]) => (
                        <div key={key} className="text-gray-300">
                          <span className="text-blue-400">{key}:</span> {value}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Request Body */}
                {selectedRequest.requestBody && (
                  <div>
                    <h3 className="font-semibold mb-2">Request Body</h3>
                    <pre className="bg-gray-800 p-2 rounded text-gray-300 overflow-x-auto">
                      {selectedRequest.requestBody}
                    </pre>
                  </div>
                )}

                {/* Response Body */}
                {selectedRequest.responseBody && (
                  <div>
                    <h3 className="font-semibold mb-2">Response Body</h3>
                    <pre className="bg-gray-800 p-2 rounded text-gray-300 overflow-x-auto max-h-96">
                      {selectedRequest.responseBody.substring(0, 5000)}
                      {selectedRequest.responseBody.length > 5000 ? '\n... (truncated)' : ''}
                    </pre>
                  </div>
                )}
              </div>
            </div>
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
            Select a request to view details
          </div>
        )}
      </div>
    </div>
  );
};
