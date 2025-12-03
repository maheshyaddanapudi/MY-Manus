import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { cn } from '../../theme';

interface CollapsibleThoughtProps {
  content: string;
}

export const CollapsibleThought: React.FC<CollapsibleThoughtProps> = ({
  content,
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  // Extract first line or first 100 chars as summary
  const getSummary = (text: string): string => {
    const firstLine = text.split('\n')[0];
    if (firstLine.length > 100) {
      return firstLine.substring(0, 100) + '...';
    }
    return firstLine || 'Thinking...';
  };

  const summary = getSummary(content);

  return (
    <div className="my-3 rounded-lg border border-purple-700/50 bg-purple-900/20 overflow-hidden">
      {/* Header - Always visible */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className={cn(
          'w-full flex items-center gap-3 px-4 py-3',
          'hover:bg-purple-700/20 transition-colors',
          'text-left'
        )}
      >
        <span className="text-lg">🤔</span>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-medium text-purple-200">Agent Thought</div>
          <div className="text-xs text-purple-300/70 truncate mt-0.5">
            {summary}
          </div>
        </div>
        <span className={cn(
          'text-purple-400 transition-transform',
          isExpanded && 'rotate-180'
        )}>
          ▼
        </span>
      </button>

      {/* Content - Collapsible */}
      {isExpanded && (
        <div className="border-t border-purple-700/50 px-4 py-3 bg-purple-900/10">
          <div className="prose prose-invert prose-sm max-w-none prose-p:text-purple-100">
            <ReactMarkdown>
              {content}
            </ReactMarkdown>
          </div>
        </div>
      )}
    </div>
  );
};
