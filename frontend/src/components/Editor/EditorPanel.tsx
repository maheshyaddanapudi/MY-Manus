import { useEffect, useState } from 'react';
import Editor from '@monaco-editor/react';
import { useAgentStore } from '../../stores/agentStore';

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
    <div className="h-full flex flex-col bg-[#1e1e1e]">
      {/* Editor toolbar */}
      <div className="h-10 bg-gray-800 border-b border-gray-700 flex items-center justify-between px-4">
        <div className="flex items-center space-x-2">
          <span className="text-sm text-gray-400">
            {displayIteration !== null ? `Iteration ${displayIteration}` : 'Current Code'}
          </span>
          {codeHistory.length > 0 && (
            <span className="text-xs text-gray-500">
              ({codeHistory.length} executions)
            </span>
          )}
        </div>

        {/* Code history dropdown */}
        {codeHistory.length > 0 && (
          <select
            value={selectedHistoryIndex}
            onChange={(e) => setSelectedHistoryIndex(Number(e.target.value))}
            className="text-xs bg-gray-700 text-gray-300 rounded px-2 py-1 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value={-1}>Current</option>
            {codeHistory.map((_, index) => (
              <option key={index} value={index}>
                Execution {index + 1} (Iter {codeHistory[index].iteration})
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
              minimap: { enabled: false },
              fontSize: 14,
              fontFamily: '"Fira Code", "Courier New", monospace',
              lineNumbers: 'on',
              renderWhitespace: 'selection',
              scrollBeyondLastLine: false,
              automaticLayout: true,
            }}
          />
        ) : (
          <div className="h-full flex items-center justify-center text-gray-500">
            <div className="text-center">
              <div className="text-4xl mb-2">📝</div>
              <p>Waiting for agent to generate code...</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
