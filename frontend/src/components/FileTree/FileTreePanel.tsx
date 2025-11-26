import React, { useState, useEffect } from 'react';
import { FileNode } from './FileNode';
import { FileViewer } from './FileViewer';
import { apiService } from '../../services/api';

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
  const [rootPath, setRootPath] = useState('/workspace');

  useEffect(() => {
    loadFileTree();
  }, [sessionId, rootPath]);

  const loadFileTree = async () => {
    setLoading(true);
    try {
      // Call file_list tool via sandbox
      const result = await apiService.executeTool('file_list', {
        path: rootPath,
        maxDepth: 3,
        includeHidden: false,
      });

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
        const result = await apiService.executeTool('file_read', { path: file.path });
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
    <div className="h-full flex bg-gray-900 text-gray-100">
      {/* File Tree */}
      <div className="w-1/3 border-r border-gray-700 overflow-y-auto">
        <div className="p-4 border-b border-gray-700">
          <h2 className="text-lg font-semibold">File Explorer</h2>
          <div className="text-xs text-gray-400 mt-1">{rootPath}</div>
          {rootPath !== '/workspace' && (
            <button
              onClick={() => setRootPath('/workspace')}
              className="mt-2 px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm"
            >
              ← Back to Workspace
            </button>
          )}
          <button
            onClick={loadFileTree}
            className="mt-2 ml-2 px-3 py-1 bg-blue-600 hover:bg-blue-500 rounded text-sm"
          >
            Refresh
          </button>
        </div>

        <div className="p-4">
          {loading && <div className="text-gray-400">Loading...</div>}

          {!loading && files.length === 0 && (
            <div className="text-gray-400">No files found</div>
          )}

          {!loading && files.length > 0 && (
            <div className="space-y-1">
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
      <div className="w-2/3">
        {selectedFile && fileContent !== null ? (
          <FileViewer filePath={selectedFile} content={fileContent} />
        ) : (
          <div className="flex items-center justify-center h-full text-gray-400">
            Select a file to view
          </div>
        )}
      </div>
    </div>
  );
};
