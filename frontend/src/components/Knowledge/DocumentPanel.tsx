import React, { useState, useEffect } from 'react';
import { apiService } from '../../services/api';

interface Document {
  id: number;
  filename: string;
  type: string;
  fileSize: number;
  uploadedAt: string;
  indexed: boolean;
  chunks?: any[];
}

interface DocumentPanelProps {
  sessionId: string;
}

export const DocumentPanel: React.FC<DocumentPanelProps> = ({ sessionId }) => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);

  useEffect(() => {
    loadDocuments();
  }, [sessionId]);

  const loadDocuments = async () => {
    setLoading(true);
    try {
      const response = await fetch(`/api/documents/${sessionId}`);
      const docs = await response.json();
      setDocuments(docs);
    } catch (error) {
      console.error('Failed to load documents:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    try {
      for (const file of Array.from(files)) {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch(`/api/documents/${sessionId}/upload`, {
          method: 'POST',
          body: formData,
        });

        if (!response.ok) {
          throw new Error(`Failed to upload ${file.name}`);
        }
      }

      // Reload documents
      await loadDocuments();
    } catch (error) {
      console.error('Upload failed:', error);
      alert('Upload failed. Please try again.');
    } finally {
      setUploading(false);
      event.target.value = ''; // Reset input
    }
  };

  const handleDelete = async (documentId: number) => {
    if (!confirm('Delete this document?')) return;

    try {
      await fetch(`/api/documents/${documentId}`, {
        method: 'DELETE',
      });
      await loadDocuments();
    } catch (error) {
      console.error('Delete failed:', error);
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`/api/documents/${sessionId}/search`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: searchQuery, topK: 5 }),
      });

      const result = await response.json();
      setSearchResults(result.chunks || []);
    } catch (error) {
      console.error('Search failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="h-full flex flex-col bg-gray-900 text-gray-100">
      {/* Header */}
      <div className="border-b border-gray-700 px-4 py-3">
        <h2 className="text-lg font-semibold">Knowledge Base</h2>
        <div className="text-xs text-gray-400 mt-1">
          Upload documents to augment agent context with RAG
        </div>
      </div>

      {/* Upload Section */}
      <div className="border-b border-gray-700 px-4 py-3">
        <label className="inline-block px-4 py-2 bg-blue-600 hover:bg-blue-500 rounded cursor-pointer text-sm">
          {uploading ? '⏳ Uploading...' : '📤 Upload Documents'}
          <input
            type="file"
            multiple
            accept=".txt,.md,.pdf,.py,.java,.js,.ts,.json,.xml"
            onChange={handleFileUpload}
            disabled={uploading}
            className="hidden"
          />
        </label>
        <div className="text-xs text-gray-400 mt-2">
          Supported: .txt, .md, .pdf, .py, .java, .js, .ts, .json, .xml
        </div>
      </div>

      {/* Search Section */}
      <div className="border-b border-gray-700 px-4 py-3">
        <div className="flex space-x-2">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            placeholder="Search knowledge base..."
            className="flex-1 px-3 py-2 bg-gray-800 border border-gray-600 rounded text-sm"
          />
          <button
            onClick={handleSearch}
            className="px-4 py-2 bg-green-600 hover:bg-green-500 rounded text-sm"
          >
            🔍 Search
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4">
        {loading && <div className="text-gray-400">Loading...</div>}

        {/* Search Results */}
        {searchResults.length > 0 && (
          <div className="mb-4">
            <h3 className="text-sm font-semibold mb-2">Search Results ({searchResults.length})</h3>
            <div className="space-y-2">
              {searchResults.map((chunk, idx) => (
                <div key={idx} className="bg-gray-800 p-3 rounded text-xs">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-blue-400 font-semibold">{chunk.filename}</span>
                    <span className="text-gray-400">Chunk {chunk.chunkIndex + 1}</span>
                  </div>
                  <pre className="text-gray-300 whitespace-pre-wrap">
                    {chunk.content.substring(0, 300)}
                    {chunk.content.length > 300 ? '...' : ''}
                  </pre>
                </div>
              ))}
            </div>
            <button
              onClick={() => setSearchResults([])}
              className="mt-2 text-xs text-gray-400 hover:text-white"
            >
              Clear results
            </button>
          </div>
        )}

        {/* Document List */}
        {!loading && documents.length === 0 && (
          <div className="text-gray-400 text-center mt-8">
            No documents uploaded yet. Upload documents to build your knowledge base.
          </div>
        )}

        {!loading && documents.length > 0 && searchResults.length === 0 && (
          <div>
            <h3 className="text-sm font-semibold mb-3">
              Documents ({documents.length})
            </h3>
            <div className="space-y-2">
              {documents.map((doc) => (
                <div
                  key={doc.id}
                  className="bg-gray-800 p-3 rounded hover:bg-gray-750 transition-colors"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center space-x-2">
                      <span className="text-blue-400">📄</span>
                      <span className="font-semibold">{doc.filename}</span>
                    </div>
                    <button
                      onClick={() => handleDelete(doc.id)}
                      className="text-red-400 hover:text-red-300 text-xs"
                    >
                      🗑️ Delete
                    </button>
                  </div>
                  <div className="flex items-center space-x-4 text-xs text-gray-400">
                    <span>{doc.type}</span>
                    <span>{formatFileSize(doc.fileSize)}</span>
                    <span>{doc.indexed ? '✅ Indexed' : '⏳ Processing'}</span>
                    {doc.chunks && <span>{doc.chunks.length} chunks</span>}
                  </div>
                  <div className="text-xs text-gray-500 mt-1">
                    Uploaded: {new Date(doc.uploadedAt).toLocaleString()}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
