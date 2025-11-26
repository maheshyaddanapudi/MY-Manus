import React, { useEffect, useState } from 'react';
import { Event } from '../../types';
import { apiService } from '../../services/api';
import EventItem from './EventItem';

interface EventStreamPanelProps {
  sessionId: string;
  autoRefresh?: boolean;
  refreshInterval?: number;
}

/**
 * Event Stream Panel - Displays the complete agent execution timeline
 * Following Manus AI's Event Stream Architecture:
 * [UserMessage → AgentThought → AgentAction → Observation → ...]
 */
const EventStreamPanel: React.FC<EventStreamPanelProps> = ({
  sessionId,
  autoRefresh = false,
  refreshInterval = 2000,
}) => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedIteration, setSelectedIteration] = useState<number | null>(null);

  // Fetch event stream
  const fetchEvents = async () => {
    if (!sessionId) return;

    try {
      setLoading(true);
      const data = selectedIteration !== null
        ? await apiService.getEventsForIteration(sessionId, selectedIteration)
        : await apiService.getEventStream(sessionId);
      setEvents(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch events');
      console.error('Error fetching events:', err);
    } finally {
      setLoading(false);
    }
  };

  // Initial fetch
  useEffect(() => {
    fetchEvents();
  }, [sessionId, selectedIteration]);

  // Auto-refresh
  useEffect(() => {
    if (!autoRefresh) return;

    const interval = setInterval(fetchEvents, refreshInterval);
    return () => clearInterval(interval);
  }, [autoRefresh, refreshInterval, sessionId, selectedIteration]);

  // Get unique iterations
  const iterations = Array.from(new Set(events.map(e => e.iteration))).sort((a, b) => a - b);

  return (
    <div className="flex flex-col h-full bg-gray-900 text-gray-100">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-700">
        <div className="flex items-center space-x-3">
          <h2 className="text-lg font-semibold">Event Stream</h2>
          <span className="px-2 py-1 text-xs bg-blue-600 rounded">
            {events.length} events
          </span>
        </div>

        {/* Iteration Filter */}
        <div className="flex items-center space-x-2">
          <label className="text-sm text-gray-400">Iteration:</label>
          <select
            value={selectedIteration ?? 'all'}
            onChange={(e) => setSelectedIteration(
              e.target.value === 'all' ? null : parseInt(e.target.value)
            )}
            className="px-3 py-1 text-sm bg-gray-800 border border-gray-600 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="all">All</option>
            {iterations.map(iter => (
              <option key={iter} value={iter}>
                Iteration {iter}
              </option>
            ))}
          </select>

          <button
            onClick={fetchEvents}
            className="px-3 py-1 text-sm bg-gray-700 hover:bg-gray-600 rounded transition-colors"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* Event Timeline */}
      <div className="flex-1 overflow-y-auto p-4">
        {loading && events.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-gray-500">Loading events...</div>
          </div>
        ) : error ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-red-500">{error}</div>
          </div>
        ) : events.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-gray-500">No events yet</div>
          </div>
        ) : (
          <div className="space-y-2">
            {events.map((event) => (
              <EventItem key={event.id} event={event} />
            ))}
          </div>
        )}
      </div>

      {/* Footer Info */}
      {events.length > 0 && (
        <div className="px-4 py-2 border-t border-gray-700 text-xs text-gray-400">
          <div className="flex items-center justify-between">
            <span>Session: {sessionId.substring(0, 8)}...</span>
            <span>
              Iterations: {iterations.length} |
              Last update: {new Date(events[events.length - 1]?.timestamp).toLocaleTimeString()}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default EventStreamPanel;
