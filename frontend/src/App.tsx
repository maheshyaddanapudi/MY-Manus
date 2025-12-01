import { useEffect, useState } from 'react';
import { MainLayout } from './components/Layout/MainLayout';
import { ChatPanel } from './components/Chat/ChatPanel';
import { TerminalPanel } from './components/Terminal/TerminalPanel';
import { EditorPanel } from './components/Editor/EditorPanel';
import { ConversationList } from './components/ConversationList';
import { useAgentStore } from './stores/agentStore';
import { websocketService } from './services/websocket';
import { apiService } from './services/api';

function App() {
  const {
    currentSessionId,
    setSessionId,
    setConnected,
    handleAgentEvent,
    loadSessions,
    createNewSession,
  } = useAgentStore();

  const [isInitializing, setIsInitializing] = useState(true);

  useEffect(() => {
    // Initialize multi-session app
    const initializeApp = async () => {
      try {
        // Check backend health
        await apiService.health();
        console.log('Backend is healthy');

        // Load all sessions
        await loadSessions();
        console.log('Loaded sessions');

        // Get fresh sessions from store (not from hook closure)
        const { sessions: loadedSessions, switchSession } = useAgentStore.getState();

        // If no sessions exist, create one
        if (loadedSessions.length === 0) {
          console.log('No sessions found, creating new session');
          await createNewSession();
        } else {
          // Use the most recent session (first in the list)
          const mostRecentSession = loadedSessions[0];
          console.log('Using most recent session:', mostRecentSession.sessionId);

          // Load the session (this loads messages from backend)
          await switchSession(mostRecentSession.sessionId);
          setSessionId(mostRecentSession.sessionId);
        }

        setIsInitializing(false);
      } catch (error) {
        console.error('Initialization error:', error);
        setIsInitializing(false);
      }
    };

    initializeApp();
  }, []);

  // Connect WebSocket when currentSessionId changes
  useEffect(() => {
    if (currentSessionId) {
      // Disconnect previous WebSocket
      websocketService.disconnect();

      // Connect to new session
      websocketService.connect(currentSessionId, (event) => {
        handleAgentEvent(event);
      });

      websocketService.onConnect(() => {
        console.log('WebSocket connected to session:', currentSessionId);
        setConnected(true);
      });

      websocketService.onDisconnect(() => {
        console.log('WebSocket disconnected');
        setConnected(false);
      });
    }

    // Cleanup
    return () => {
      websocketService.disconnect();
    };
  }, [currentSessionId]);

  if (isInitializing) {
    return (
      <div className="h-screen flex items-center justify-center bg-gray-900 text-white">
        <div className="text-center">
          <div className="text-6xl mb-4">🤖</div>
          <h2 className="text-2xl font-bold mb-2">Initializing MY Manus...</h2>
          <p className="text-gray-400">Connecting to backend</p>
        </div>
      </div>
    );
  }

  return (
    <MainLayout
      conversationListPanel={<ConversationList />}
      chatPanel={<ChatPanel />}
      terminalPanel={<TerminalPanel />}
      editorPanel={<EditorPanel />}
    />
  );
}

export default App;
