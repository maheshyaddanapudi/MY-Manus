import { useState } from 'react';
import { cn } from '../../theme';

interface CollapsibleObservationProps {
  content: string;
  isError?: boolean;
}

export const CollapsibleObservation = ({ content, isError = false }: CollapsibleObservationProps) => {
  const lines = content.split('\n');
  const isLarge = lines.length > 10 || content.length > 500;
  const [isExpanded, setIsExpanded] = useState(!isLarge);

  // For small outputs, show directly
  if (!isLarge) {
    return (
      <div className={cn(
        "mt-1 ml-4 p-3 rounded-lg border",
        isError 
          ? "bg-red-900/20 border-red-700/50" 
          : "bg-gray-900/50 border-gray-700/50"
      )}>
        <div className="flex items-start gap-2">
          <span className={cn(
            "text-sm font-semibold",
            isError ? "text-red-400" : "text-green-400"
          )}>
            {isError ? '✗ Error:' : '✓ Output:'}
          </span>
          <pre className="text-xs text-gray-300 whitespace-pre-wrap flex-1 font-mono">
            {content}
          </pre>
        </div>
      </div>
    );
  }

  // For large outputs, make it collapsible
  return (
    <div className={cn(
      "mt-1 ml-4 rounded-lg border",
      isError 
        ? "bg-red-900/20 border-red-700/50" 
        : "bg-gray-900/50 border-gray-700/50"
    )}>
      {/* Header - Always visible */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className={cn(
          "w-full px-3 py-2 flex items-center justify-between",
          "hover:bg-gray-800/50 transition-colors rounded-lg"
        )}
      >
        <div className="flex items-center gap-2">
          <span className={cn(
            "text-sm font-semibold",
            isError ? "text-red-400" : "text-green-400"
          )}>
            {isError ? '✗ Error:' : '✓ Output:'}
          </span>
          <span className="text-xs text-gray-500">
            ({lines.length} lines, {content.length} chars)
          </span>
        </div>
        <span className="text-gray-400 text-sm">
          {isExpanded ? '▼' : '▶'}
        </span>
      </button>

      {/* Content - Collapsible */}
      {isExpanded && (
        <div className="px-3 pb-3 pt-1 border-t border-gray-700/30">
          <pre className="text-xs text-gray-300 whitespace-pre-wrap font-mono max-h-96 overflow-y-auto">
            {content}
          </pre>
        </div>
      )}
    </div>
  );
};
