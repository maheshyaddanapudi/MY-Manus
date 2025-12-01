import React, { useEffect, useState } from 'react';
import { useAgentStore } from '../../stores/agentStore';
import type { Session } from '../../types';
import { PlusIcon, TrashIcon, PencilIcon, CheckIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { ChatBubbleLeftIcon } from '@heroicons/react/24/solid';

export const ConversationList: React.FC = () => {
  const {
    sessions,
    currentSessionId,
    loadSessions,
    createNewSession,
    switchSession,
    deleteSession,
    renameSession,
  } = useAgentStore();

  const [editingSessionId, setEditingSessionId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    loadSessions();
  }, [loadSessions]);

  const handleNewChat = async () => {
    setIsCreating(true);
    try {
      await createNewSession();
    } finally {
      setIsCreating(false);
    }
  };

  const handleSwitchSession = (sessionId: string) => {
    if (sessionId !== currentSessionId) {
      switchSession(sessionId);
    }
  };

  const handleDelete = async (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm('Delete this conversation?')) {
      await deleteSession(sessionId);
    }
  };

  const startEditing = (session: Session, e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingSessionId(session.sessionId);
    setEditTitle(session.title);
  };

  const cancelEditing = (e: React.MouseEvent) => {
    e.stopPropagation();
    setEditingSessionId(null);
    setEditTitle('');
  };

  const saveEdit = async (sessionId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (editTitle.trim()) {
      await renameSession(sessionId, editTitle.trim());
      setEditingSessionId(null);
      setEditTitle('');
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) return 'Today';
    if (days === 1) return 'Yesterday';
    if (days < 7) return `${days} days ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="flex flex-col h-full bg-gray-900 border-r border-gray-700">
      {/* Header */}
      <div className="p-4 border-b border-gray-700">
        <button
          onClick={handleNewChat}
          disabled={isCreating}
          className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50"
        >
          <PlusIcon className="w-5 h-5" />
          <span>{isCreating ? 'Creating...' : 'New Chat'}</span>
        </button>
      </div>

      {/* Conversation List */}
      <div className="flex-1 overflow-y-auto">
        {sessions.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            <ChatBubbleLeftIcon className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p className="text-sm">No conversations yet</p>
            <p className="text-xs mt-1">Click "New Chat" to start</p>
          </div>
        ) : (
          <div className="p-2 space-y-1">
            {sessions.map((session) => (
              <div
                key={session.sessionId}
                onClick={() => handleSwitchSession(session.sessionId)}
                className={`
                  group relative px-3 py-3 rounded-lg cursor-pointer transition-colors
                  ${
                    currentSessionId === session.sessionId
                      ? 'bg-gray-800 text-white'
                      : 'text-gray-300 hover:bg-gray-800/50'
                  }
                `}
              >
                {editingSessionId === session.sessionId ? (
                  // Edit mode
                  <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                    <input
                      type="text"
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      onClick={(e) => e.stopPropagation()}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') saveEdit(session.sessionId, e as any);
                        if (e.key === 'Escape') cancelEditing(e as any);
                      }}
                      className="flex-1 px-2 py-1 text-sm bg-gray-700 text-white rounded border border-gray-600 focus:outline-none focus:border-blue-500"
                      autoFocus
                    />
                    <button
                      onClick={(e) => saveEdit(session.sessionId, e)}
                      className="p-1 hover:bg-gray-700 rounded"
                    >
                      <CheckIcon className="w-4 h-4 text-green-400" />
                    </button>
                    <button
                      onClick={cancelEditing}
                      className="p-1 hover:bg-gray-700 rounded"
                    >
                      <XMarkIcon className="w-4 h-4 text-red-400" />
                    </button>
                  </div>
                ) : (
                  // Normal mode
                  <>
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <ChatBubbleLeftIcon className="w-4 h-4 flex-shrink-0" />
                          <h3 className="font-medium text-sm truncate">{session.title}</h3>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                          {formatDate(session.updatedAt)} · {session.messageCount} messages
                        </p>
                      </div>

                      {/* Action buttons (show on hover) */}
                      <div className="flex-shrink-0 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={(e) => startEditing(session, e)}
                          className="p-1.5 hover:bg-gray-700 rounded transition-colors"
                          title="Rename"
                        >
                          <PencilIcon className="w-4 h-4" />
                        </button>
                        <button
                          onClick={(e) => handleDelete(session.sessionId, e)}
                          className="p-1.5 hover:bg-red-900/50 rounded transition-colors"
                          title="Delete"
                        >
                          <TrashIcon className="w-4 h-4 text-red-400" />
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
