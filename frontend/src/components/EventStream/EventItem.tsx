import React, { useState } from 'react';
import type { Event, EventType } from '../../types';

interface EventItemProps {
  event: Event;
}

/**
 * Event Item - Displays a single event in the timeline
 */
const EventItem: React.FC<EventItemProps> = ({ event }) => {
  const [expanded, setExpanded] = useState(false);

  // Get event type styling
  const getEventStyle = (type: EventType) => {
    switch (type) {
      case 'USER_MESSAGE':
        return {
          bg: 'bg-blue-900/30',
          border: 'border-blue-500',
          icon: '💬',
          label: 'User Message',
          textColor: 'text-blue-400',
        };
      case 'AGENT_THOUGHT':
        return {
          bg: 'bg-purple-900/30',
          border: 'border-purple-500',
          icon: '🤔',
          label: 'Agent Thought',
          textColor: 'text-purple-400',
        };
      case 'AGENT_ACTION':
        return {
          bg: 'bg-green-900/30',
          border: 'border-green-500',
          icon: '⚡',
          label: 'Agent Action',
          textColor: 'text-green-400',
        };
      case 'OBSERVATION':
        return {
          bg: event.success ? 'bg-cyan-900/30' : 'bg-red-900/30',
          border: event.success ? 'border-cyan-500' : 'border-red-500',
          icon: event.success ? '✅' : '❌',
          label: 'Observation',
          textColor: event.success ? 'text-cyan-400' : 'text-red-400',
        };
      case 'AGENT_RESPONSE':
        return {
          bg: 'bg-indigo-900/30',
          border: 'border-indigo-500',
          icon: '💭',
          label: 'Agent Response',
          textColor: 'text-indigo-400',
        };
      case 'SYSTEM':
        return {
          bg: 'bg-gray-900/30',
          border: 'border-gray-500',
          icon: '⚙️',
          label: 'System',
          textColor: 'text-gray-400',
        };
      case 'ERROR':
        return {
          bg: 'bg-red-900/30',
          border: 'border-red-500',
          icon: '🚨',
          label: 'Error',
          textColor: 'text-red-400',
        };
      default:
        return {
          bg: 'bg-gray-900/30',
          border: 'border-gray-500',
          icon: '📋',
          label: 'Unknown',
          textColor: 'text-gray-400',
        };
    }
  };

  const style = getEventStyle(event.type);

  return (
    <div
      className={`${style.bg} border-l-4 ${style.border} rounded p-3 cursor-pointer hover:bg-opacity-50 transition-all`}
      onClick={() => setExpanded(!expanded)}
    >
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-start space-x-3 flex-1">
          <span className="text-2xl">{style.icon}</span>
          <div className="flex-1 min-w-0">
            <div className="flex items-center space-x-2">
              <span className={`font-semibold ${style.textColor}`}>
                {style.label}
              </span>
              <span className="text-xs text-gray-500">
                Iteration {event.iteration}
              </span>
              {event.durationMs && (
                <span className="text-xs text-gray-500">
                  {event.durationMs}ms
                </span>
              )}
            </div>

            {/* Content Preview */}
            {!expanded && (
              <div className="mt-1 text-sm text-gray-300 truncate">
                {event.content?.substring(0, 100)}
                {event.content?.length > 100 && '...'}
              </div>
            )}
          </div>
        </div>

        {/* Timestamp */}
        <span className="text-xs text-gray-500 ml-2">
          {new Date(event.timestamp).toLocaleTimeString()}
        </span>
      </div>

      {/* Expanded Content */}
      {expanded && (
        <div className="mt-3 space-y-2">
          {/* Content */}
          {event.content && (
            <div className="bg-gray-800/50 rounded p-3">
              <div className="text-xs text-gray-400 mb-1">Content:</div>
              <pre className="text-sm text-gray-200 whitespace-pre-wrap font-mono">
                {event.content}
              </pre>
            </div>
          )}

          {/* Error */}
          {event.error && (
            <div className="bg-red-900/30 border border-red-500 rounded p-3">
              <div className="text-xs text-red-400 mb-1">Error:</div>
              <pre className="text-sm text-red-300 whitespace-pre-wrap font-mono">
                {event.error}
              </pre>
            </div>
          )}

          {/* Data */}
          {event.data && Object.keys(event.data).length > 0 && (
            <div className="bg-gray-800/50 rounded p-3">
              <div className="text-xs text-gray-400 mb-1">Metadata:</div>
              <pre className="text-xs text-gray-300 whitespace-pre-wrap font-mono overflow-x-auto">
                {JSON.stringify(event.data, null, 2)}
              </pre>
            </div>
          )}

          {/* Event Info */}
          <div className="flex items-center space-x-4 text-xs text-gray-500">
            <span>ID: {event.id.substring(0, 8)}...</span>
            <span>Sequence: {event.sequence}</span>
            {event.success !== undefined && (
              <span>Success: {event.success ? '✓' : '✗'}</span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default EventItem;
