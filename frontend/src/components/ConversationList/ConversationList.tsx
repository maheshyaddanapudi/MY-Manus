import { useState, useEffect } from 'react';
import { useAgentStore } from '../../stores/agentStore';
import type { Session } from '../../types';
import { PlusIcon, TrashIcon, PencilIcon, CheckIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { ChatBubbleLeftIcon } from '@heroicons/react/24/solid';
import { getButtonClasses, cn } from '../../theme';

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
    <div className="flex flex-col h-full bg-gradient-to-b from-gray-900 to-gray-900/95 border-r border-gray-700/50">
      {/* Header */}
      <div className="p-4 border-b border-gray-700/50">
        <button
          onClick={handleNewChat}
          disabled={isCreating}
          className={cn(
            getButtonClasses('primary', 'md'),
            'w-full shadow-lg hover:shadow-xl',
            isCreating && 'opacity-50 cursor-not-allowed'
          )}
        >
          <PlusIcon className="w-5 h-5" />
          <span className="font-semibold">{isCreating ? 'Creating...' : 'New Chat'}</span>
        </button>
      </div>

      {/* Conversation List */}
      <div className="flex-1 overflow-y-auto custom-scrollbar">
        {sessions.length === 0 ? (
          <div className="p-8 text-center">
            <div className="w-16 h-16 mx-auto mb-4 bg-gray-800/50 rounded-2xl flex items-center justify-center border border-gray-700/50">
              <ChatBubbleLeftIcon className="w-8 h-8 text-gray-600" />
            </div>
            <p className="text-sm text-gray-400 font-medium">No conversations yet</p>
            <p className="text-xs text-gray-500 mt-1">Click "New Chat" to start</p>
          </div>
        ) : (
          <div className="p-3 space-y-1.5">
            {sessions.map((session) => (
              <div
                key={session.sessionId}
                onClick={() => handleSwitchSession(session.sessionId)}
                className={cn(
                  'group relative px-3.5 py-3 rounded-xl cursor-pointer transition-all duration-200',
                  currentSessionId === session.sessionId
                    ? 'bg-gradient-to-r from-blue-600/20 to-purple-600/20 border border-blue-500/30 shadow-lg shadow-blue-500/10'
                    : 'hover:bg-gray-800/60 border border-transparent hover:border-gray-700/50'
                )}
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
                      className="flex-1 px-3 py-1.5 text-sm bg-gray-800 text-white rounded-lg border border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all"
                      autoFocus
                    />
                    <button
                      onClick={(e) => saveEdit(session.sessionId, e)}
                      className="p-1.5 hover:bg-green-900/30 rounded-lg transition-colors"
                      title="Save"
                    >
                      <CheckIcon className="w-4 h-4 text-green-400" />
                    </button>
                    <button
                      onClick={cancelEditing}
                      className="p-1.5 hover:bg-red-900/30 rounded-lg transition-colors"
                      title="Cancel"
                    >
                      <XMarkIcon className="w-4 h-4 text-red-400" />
                    </button>
                  </div>
                ) : (
                  // Normal mode
                  <>
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2.5 mb-1.5">
                          <div className={cn(
                            'w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 transition-all',
                            currentSessionId === session.sessionId
                              ? 'bg-gradient-to-br from-blue-500 to-purple-600 shadow-lg'
                              : 'bg-gray-800 border border-gray-700'
                          )}>
                            <ChatBubbleLeftIcon className="w-4 h-4 text-white" />
                          </div>
                          <h3 className={cn(
                            'font-semibold text-sm truncate transition-colors',
                            currentSessionId === session.sessionId
                              ? 'text-white'
                              : 'text-gray-300 group-hover:text-white'
                          )}>
                            {session.title}
                          </h3>
                        </div>
                        <div className="flex items-center gap-2 ml-10">
                          <span className="text-xs text-gray-500 font-medium">
                            {formatDate(session.updatedAt)}
                          </span>
                          <span className="text-gray-600">·</span>
                          <span className="text-xs text-gray-500">
                            {session.messageCount} {session.messageCount === 1 ? 'message' : 'messages'}
                          </span>
                        </div>
                      </div>

                      {/* Action buttons (show on hover) */}
                      <div className="flex-shrink-0 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={(e) => startEditing(session, e)}
                          className="p-1.5 hover:bg-gray-700/80 rounded-lg transition-all hover:scale-110"
                          title="Rename"
                        >
                          <PencilIcon className="w-4 h-4 text-gray-400 hover:text-blue-400 transition-colors" />
                        </button>
                        <button
                          onClick={(e) => handleDelete(session.sessionId, e)}
                          className="p-1.5 hover:bg-red-900/40 rounded-lg transition-all hover:scale-110"
                          title="Delete"
                        >
                          <TrashIcon className="w-4 h-4 text-gray-400 hover:text-red-400 transition-colors" />
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

      {/* Custom Scrollbar Styles */}
      <style>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 6px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: transparent;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: #475569;
          border-radius: 3px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: #64748b;
        }
      `}</style>
    </div>
  );
};
