import { useEffect, useState } from 'react';
import { MainLayout } from './components/Layout/MainLayout';
import { ChatPanel } from './components/Chat/ChatPanel';
import { TerminalPanel } from './components/Terminal/TerminalPanel';
import { EditorPanel } from './components/Editor/EditorPanel';
import { useAgentStore } from './stores/agentStore';
import { websocketService } from './services/websocket';
import { apiService } from './services/api';

function App() {
  const {
    sessionId,
    setSessionId,
    setConnected,
    handleAgentEvent,
  } = useAgentStore();

  const [isInitializing, setIsInitializing] = useState(true);

  useEffect(() => {
    // Initialize session
    const initializeSession = async () => {
      try {
        // Check backend health
        await apiService.health();
        console.log('Backend is healthy');

        // Create or retrieve session
        let sid = sessionId;
        if (!sid) {
          const response = await apiService.createSession();
          sid = response.sessionId;
          setSessionId(sid);
          console.log('Created new session:', sid);
        }

        // Connect WebSocket
        websocketService.connect(sid, (event) => {
          handleAgentEvent(event);
        });

        websocketService.onConnect(() => {
          console.log('WebSocket connected');
          setConnected(true);
        });

        websocketService.onDisconnect(() => {
          console.log('WebSocket disconnected');
          setConnected(false);
        });

        setIsInitializing(false);
      } catch (error) {
        console.error('Initialization error:', error);
        setIsInitializing(false);
      }
    };

    initializeSession();

    // Cleanup
    return () => {
      websocketService.disconnect();
    };
  }, []);

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
      chatPanel={<ChatPanel />}
      terminalPanel={<TerminalPanel />}
      editorPanel={<EditorPanel />}
    />
  );
}

export default App;
