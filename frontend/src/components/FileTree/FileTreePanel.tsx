import React, { useState, useEffect } from 'react';
import { FileNode } from './FileNode';
import { FileViewer } from './FileViewer';
import { apiService } from '../../services/api';
import { cn, getButtonClasses } from '../../theme';

interface FileInfo {
  path: string;
  name: string;
  type: 'file' | 'directory';
  size: number;
  lastModified: number;
  depth: number;
  extension?: string;
}

interface FileTreePanelProps {
  sessionId: string;
}

export const FileTreePanel: React.FC<FileTreePanelProps> = ({ sessionId }) => {
  const [files, setFiles] = useState<FileInfo[]>([]);
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [fileContent, setFileContent] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [rootPath, setRootPath] = useState<string>('.');

  useEffect(() => {
    loadFileTree();
  }, [rootPath, sessionId]);

  const loadFileTree = async () => {
    setLoading(true);
    try {
      // Call file browsing API with sessionId and relative path
      const result = await apiService.listFiles(sessionId, rootPath, 3, false);

      if (result.success) {
        setFiles(result.files || []);
      }
    } catch (error) {
      console.error('Failed to load file tree:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = async (file: FileInfo) => {
    if (file.type === 'directory') {
      // Expand/collapse directory or navigate
      setRootPath(file.path);
    } else {
      // Load file content
      setSelectedFile(file.path);
      setLoading(true);
      try {
        const result = await apiService.readFile(sessionId, file.path);
        if (result.success) {
          setFileContent(result.content);
        }
      } catch (error) {
        console.error('Failed to load file:', error);
      } finally {
        setLoading(false);
      }
    }
  };

  const buildTree = (files: FileInfo[]) => {
    // Group files by depth for hierarchical display
    const tree: Record<string, FileInfo[]> = {};

    files.forEach((file) => {
      const depth = file.depth || 0;
      if (!tree[depth]) {
        tree[depth] = [];
      }
      tree[depth].push(file);
    });

    return tree;
  };

  const tree = buildTree(files);

  return (
    <div className="h-full flex bg-gradient-to-b from-gray-900 to-gray-900/95">
      {/* File Tree */}
      <div className="w-1/3 border-r border-gray-700/50 overflow-y-auto custom-scrollbar">
        <div className="p-4 border-b border-gray-700/50 bg-gray-800/40 backdrop-blur-sm">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xl">📂</span>
            <h2 className="text-base font-semibold text-gray-200">File Explorer</h2>
          </div>
          <div className="text-xs text-gray-500 font-mono bg-gray-800/50 px-2 py-1 rounded mt-2">
            Session: {sessionId} / {rootPath}
          </div>
          <div className="flex gap-2 mt-3">
            {rootPath !== '.' && (
              <button
                onClick={() => setRootPath('.')}
                className={cn(getButtonClasses('secondary', 'sm'))}
              >
                ← Back
              </button>
            )}
            <button
              onClick={loadFileTree}
              className={cn(getButtonClasses('primary', 'sm'))}
            >
              🔄 Refresh
            </button>
          </div>
        </div>

        <div className="p-3">
          {loading && (
            <div className="text-center py-8">
              <div className="text-3xl mb-2 animate-pulse">📂</div>
              <div className="text-gray-400 text-sm">Loading files...</div>
            </div>
          )}

          {!loading && files.length === 0 && (
            <div className="text-center py-12">
              <div className="text-5xl mb-3 opacity-30">📁</div>
              <div className="text-gray-400">No files found</div>
              <div className="text-gray-600 text-sm mt-1">Directory is empty</div>
            </div>
          )}

          {!loading && files.length > 0 && (
            <div className="space-y-0.5">
              {Object.entries(tree).map(([depth, depthFiles]) => (
                <div key={depth}>
                  {depthFiles.map((file) => (
                    <FileNode
                      key={file.path}
                      file={file}
                      onSelect={handleFileSelect}
                      isSelected={selectedFile === file.path}
                    />
                  ))}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* File Viewer */}
      <div className="w-2/3 bg-gray-900/50">
        {selectedFile && fileContent !== null ? (
          <FileViewer filePath={selectedFile} content={fileContent} />
        ) : (
          <div className="flex items-center justify-center h-full">
            <div className="text-center space-y-4">
              <div className="text-6xl opacity-30">📄</div>
              <div>
                <p className="text-lg font-medium text-gray-400">No File Selected</p>
                <p className="text-sm text-gray-600 mt-2">
                  Select a file from the tree to view its contents
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
