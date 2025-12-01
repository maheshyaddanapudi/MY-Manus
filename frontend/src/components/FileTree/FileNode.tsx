import React from 'react';
import { FolderIcon, DocumentIcon } from '@heroicons/react/24/outline';

interface FileInfo {
  path: string;
  name: string;
  type: 'file' | 'directory';
  size: number;
  lastModified: number;
  depth: number;
  extension?: string;
}

interface FileNodeProps {
  file: FileInfo;
  onSelect: (file: FileInfo) => any; // Support both sync and async (void or Promise<void>)
  isSelected: boolean;
}

export const FileNode: React.FC<FileNodeProps> = ({ file, onSelect, isSelected }) => {
  const indent = file.depth * 16; // 16px per level

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const getFileIcon = () => {
    if (file.type === 'directory') {
      return <FolderIcon className="w-4 h-4 text-blue-400" />;
    }

    // Color-code by extension
    const iconColor = getIconColor(file.extension || '');
    return <DocumentIcon className={`w-4 h-4 ${iconColor}`} />;
  };

  const getIconColor = (extension: string) => {
    const ext = extension.toLowerCase();
    if (['js', 'jsx', 'ts', 'tsx'].includes(ext)) return 'text-yellow-400';
    if (['py'].includes(ext)) return 'text-blue-400';
    if (['java'].includes(ext)) return 'text-red-400';
    if (['html', 'css', 'scss'].includes(ext)) return 'text-purple-400';
    if (['json', 'xml', 'yml', 'yaml'].includes(ext)) return 'text-green-400';
    if (['md', 'txt'].includes(ext)) return 'text-gray-400';
    return 'text-gray-500';
  };

  return (
    <div
      onClick={async () => await onSelect(file)}
      className={`flex items-center px-2 py-1 cursor-pointer hover:bg-gray-800 rounded transition-colors ${
        isSelected ? 'bg-gray-700' : ''
      }`}
      style={{ paddingLeft: `${indent + 8}px` }}
    >
      <div className="flex items-center flex-1 min-w-0">
        <div className="flex-shrink-0 mr-2">{getFileIcon()}</div>
        <div className="flex-1 min-w-0">
          <div className="text-sm truncate">{file.name}</div>
        </div>
        {file.type === 'file' && (
          <div className="text-xs text-gray-500 ml-2">{formatSize(file.size)}</div>
        )}
      </div>
    </div>
  );
};
