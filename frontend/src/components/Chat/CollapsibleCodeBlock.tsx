import React, { useState } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { cn } from '../../theme';

interface CollapsibleCodeBlockProps {
  code: string;
  language: string;
}

export const CollapsibleCodeBlock: React.FC<CollapsibleCodeBlockProps> = ({
  code,
  language,
}) => {
  const [isExpanded, setIsExpanded] = useState(false);

  // Detect action type from code
  const detectAction = (code: string): { icon: string; title: string; detail?: string } => {
    // Check for file operations
    if (code.includes('file_write(') || code.includes('with open(')) {
      const fileMatch = code.match(/file_write\(['"]([^'"]+)['"]/);
      const openMatch = code.match(/open\(['"]([^'"]+)['"]/);
      const filePath = fileMatch?.[1] || openMatch?.[1] || 'file';
      return { icon: '✏️', title: 'Editing file', detail: filePath };
    }

    // Check for shell commands
    if (code.includes('shell(') || code.includes('subprocess') || code.includes('os.system')) {
      const cmdMatch = code.match(/shell\(['"]([^'"]+)['"]/);
      const cmd = cmdMatch?.[1] || 'command';
      return { icon: '⌨️', title: 'Executing command', detail: cmd };
    }

    // Check for todo operations
    if (code.includes('todo(')) {
      return { icon: '📝', title: 'Updating todo', detail: 'todo.md' };
    }

    // Check for data analysis
    if (code.includes('pandas') || code.includes('DataFrame') || code.includes('csv')) {
      return { icon: '📊', title: 'Analyzing data' };
    }

    // Check for API calls
    if (code.includes('requests.') || code.includes('http')) {
      return { icon: '🌐', title: 'Making API call' };
    }

    // Default
    return { icon: '🔧', title: 'Executing code' };
  };

  const action = detectAction(code);

  return (
    <div className="my-3 rounded-lg border border-gray-700/50 bg-gray-800/30 overflow-hidden">
      {/* Header - Always visible */}
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className={cn(
          'w-full flex items-center gap-3 px-4 py-3',
          'hover:bg-gray-700/30 transition-colors',
          'text-left'
        )}
      >
        <span className="text-lg">{action.icon}</span>
        <div className="flex-1 min-w-0">
          <div className="text-sm font-medium text-gray-200">{action.title}</div>
          {action.detail && (
            <div className="text-xs text-gray-400 font-mono truncate mt-0.5">
              {action.detail}
            </div>
          )}
        </div>
        <span className={cn(
          'text-gray-400 transition-transform',
          isExpanded && 'rotate-180'
        )}>
          ▼
        </span>
      </button>

      {/* Code - Collapsible */}
      {isExpanded && (
        <div className="border-t border-gray-700/50">
          <SyntaxHighlighter
            style={vscDarkPlus as any}
            language={language}
            PreTag="div"
            customStyle={{
              margin: 0,
              fontSize: '0.875rem',
              background: 'transparent',
            }}
          >
            {code}
          </SyntaxHighlighter>
        </div>
      )}
    </div>
  );
};
