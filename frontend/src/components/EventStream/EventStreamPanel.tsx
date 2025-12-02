import React, { useState, useMemo } from 'react';
import { useAgentStore } from '../../stores/agentStore';
import EventItem from './EventItem';
import { cn, getBadgeClasses } from '../../theme';

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
  sessionId: _sessionId,
}) => {
  const { events: allEvents } = useAgentStore();
  const [selectedIteration, setSelectedIteration] = useState<number | null>(null);

  // Filter events by iteration if selected
  const events = useMemo(() => {
    if (selectedIteration === null) return allEvents;
    return allEvents.filter(e => e.iteration === selectedIteration);
  }, [allEvents, selectedIteration]);

  // Get unique iterations
  const iterations = Array.from(new Set(events.map(e => e.iteration))).sort((a, b) => a - b);

  return (
    <div className="flex flex-col h-full bg-gradient-to-b from-gray-900 to-gray-900/95">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-700/50 bg-gray-800/40 backdrop-blur-sm">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <span className="text-xl">📊</span>
            <h2 className="text-base font-semibold text-gray-200">Event Stream</h2>
          </div>
          <span className={cn(getBadgeClasses('info'), 'text-xs')}>
            {events.length} {events.length === 1 ? 'event' : 'events'}
          </span>
        </div>

        {/* Iteration Filter */}
        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-400 font-medium">Filter:</label>
          <select
            value={selectedIteration ?? 'all'}
            onChange={(e) => setSelectedIteration(
              e.target.value === 'all' ? null : parseInt(e.target.value)
            )}
            className={cn(
              'text-xs bg-gray-800 text-gray-300 rounded-lg px-3 py-1.5',
              'border border-gray-700 hover:border-gray-600',
              'focus:outline-none focus:ring-2 focus:ring-blue-500/50',
              'transition-colors cursor-pointer'
            )}
          >
            <option value="all">🔄 All Iterations</option>
            {iterations.map(iter => (
              <option key={iter} value={iter}>
                #{iter} Iteration {iter}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Event Timeline */}
      <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">
        {events.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-center space-y-4">
              <div className="text-6xl opacity-30">📊</div>
              <div>
                <p className="text-lg font-medium text-gray-400">No Events Yet</p>
                <p className="text-sm text-gray-600 mt-2">
                  Events will appear here as the agent executes
                </p>
              </div>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            {events.map((event) => (
              <EventItem key={event.id} event={event} />
            ))}
          </div>
        )}
      </div>

      {/* Footer Info */}
      {events.length > 0 && (
        <div className="px-4 py-2.5 border-t border-gray-700/50 bg-gray-800/30 backdrop-blur-sm">
          <div className="flex items-center justify-between text-xs text-gray-400">
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                <span className="font-medium">{iterations.length}</span> iterations
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-purple-500"></span>
                <span className="font-medium">{events.length}</span> events
              </span>
            </div>
            <span className="text-gray-500">
              Last: {new Date(events[events.length - 1]?.timestamp).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
              })}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default EventStreamPanel;
