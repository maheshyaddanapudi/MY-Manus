import { useState, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import { useAgentStore } from '../../stores/agentStore';
import { cn, getBadgeClasses } from '../../theme';

export const EditorPanel = () => {
  const { currentCode, codeHistory } = useAgentStore();
  const [selectedHistoryIndex, setSelectedHistoryIndex] = useState<number>(-1);

  // Auto-select latest code when new code arrives
  useEffect(() => {
    setSelectedHistoryIndex(-1); // -1 means show current code
  }, [currentCode]);

  const displayCode =
    selectedHistoryIndex >= 0 ? codeHistory[selectedHistoryIndex]?.code : currentCode;

  const displayIteration =
    selectedHistoryIndex >= 0 ? codeHistory[selectedHistoryIndex]?.iteration : null;

  return (
    <div className="h-full flex flex-col bg-gradient-to-b from-gray-900 to-gray-900/95">
      {/* Editor toolbar */}
      <div className="h-12 bg-gray-800/60 border-b border-gray-700/50 flex items-center justify-between px-4 backdrop-blur-sm">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <span className="text-2xl">📝</span>
            <span className="text-sm text-gray-300 font-medium">
              {displayIteration !== null ? `Iteration ${displayIteration}` : 'Code Editor'}
            </span>
          </div>
          {codeHistory.length > 0 && (
            <span className={cn(getBadgeClasses('info'), 'text-xs')}>
              {codeHistory.length} {codeHistory.length === 1 ? 'execution' : 'executions'}
            </span>
          )}
        </div>

        {/* Code history dropdown */}
        {codeHistory.length > 0 && (
          <select
            value={selectedHistoryIndex}
            onChange={(e) => setSelectedHistoryIndex(Number(e.target.value))}
            className={cn(
              'text-xs bg-gray-800 text-gray-300 rounded-lg px-3 py-1.5',
              'border border-gray-700 hover:border-gray-600',
              'focus:outline-none focus:ring-2 focus:ring-blue-500/50',
              'transition-colors cursor-pointer'
            )}
          >
            <option value={-1}>📄 Current</option>
            {codeHistory.map((_, index) => (
              <option key={index} value={index}>
                🔄 Execution {index + 1} (Iteration {codeHistory[index].iteration})
              </option>
            ))}
          </select>
        )}
      </div>

      {/* Monaco Editor */}
      <div className="flex-1">
        {displayCode ? (
          <Editor
            height="100%"
            defaultLanguage="python"
            value={displayCode}
            theme="vs-dark"
            options={{
              readOnly: true,
              minimap: { enabled: true },
              fontSize: 13,
              fontFamily: '"SF Mono", "Monaco", "Inconsolata", "Fira Code", "Courier New", monospace',
              lineNumbers: 'on',
              renderWhitespace: 'selection',
              scrollBeyondLastLine: false,
              automaticLayout: true,
              padding: { top: 16, bottom: 16 },
              lineHeight: 20,
              fontWeight: '400',
              cursorBlinking: 'smooth',
              cursorSmoothCaretAnimation: 'on',
              smoothScrolling: true,
            }}
          />
        ) : (
          <div className="h-full flex items-center justify-center text-gray-500">
            <div className="text-center space-y-4">
              <div className="text-6xl opacity-50">📝</div>
              <div>
                <p className="text-lg font-medium text-gray-400">No Code Yet</p>
                <p className="text-sm text-gray-600 mt-2">
                  Waiting for agent to generate code...
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
