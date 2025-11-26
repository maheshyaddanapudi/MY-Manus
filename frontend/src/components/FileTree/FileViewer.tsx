import React from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

interface FileViewerProps {
  filePath: string;
  content: string;
}

export const FileViewer: React.FC<FileViewerProps> = ({ filePath, content }) => {
  const getLanguage = (path: string) => {
    const ext = path.split('.').pop()?.toLowerCase();
    const langMap: Record<string, string> = {
      js: 'javascript',
      jsx: 'jsx',
      ts: 'typescript',
      tsx: 'tsx',
      py: 'python',
      java: 'java',
      html: 'html',
      css: 'css',
      scss: 'scss',
      json: 'json',
      xml: 'xml',
      yml: 'yaml',
      yaml: 'yaml',
      md: 'markdown',
      sh: 'bash',
      sql: 'sql',
    };
    return langMap[ext || ''] || 'text';
  };

  const language = getLanguage(filePath);
  const fileName = filePath.split('/').pop() || filePath;

  return (
    <div className="h-full flex flex-col bg-gray-900">
      {/* Header */}
      <div className="border-b border-gray-700 px-4 py-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold text-gray-100">{fileName}</h3>
            <div className="text-xs text-gray-400 mt-1">{filePath}</div>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-xs text-gray-400 px-2 py-1 bg-gray-800 rounded">
              {language}
            </span>
            <span className="text-xs text-gray-400">
              {content.split('\n').length} lines
            </span>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto">
        <SyntaxHighlighter
          language={language}
          style={vscDarkPlus}
          showLineNumbers
          customStyle={{
            margin: 0,
            padding: '1rem',
            background: '#1e1e1e',
            fontSize: '13px',
          }}
          lineNumberStyle={{
            minWidth: '3em',
            paddingRight: '1em',
            color: '#858585',
          }}
        >
          {content}
        </SyntaxHighlighter>
      </div>
    </div>
  );
};
