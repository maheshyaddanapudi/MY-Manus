# UI Implementation Guide

## Overview

The Manus AI clone frontend is built with React 18, TypeScript, Tailwind CSS, and Zustand for state management. It provides a transparent three-panel interface showing agent execution in real-time.

## Technology Stack

**Location**: `/frontend/package.json`

```json
{
  "dependencies": {
    "@monaco-editor/react": "^4.7.0",
    "@stomp/stompjs": "^7.2.1",
    "@xterm/xterm": "^5.5.0",
    "axios": "^1.13.2",
    "react": "^19.2.0",
    "react-dom": "^19.2.0",
    "react-markdown": "^10.1.0",
    "react-syntax-highlighter": "^16.1.0",
    "zustand": "^5.0.8"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^5.1.1",
    "tailwindcss": "^4.1.17",
    "typescript": "~5.9.3",
    "vite": "^7.2.4"
  }
}
```

## Architecture

### Three-Panel Layout

**Location**: `/frontend/src/components/Layout/MainLayout.tsx`

```tsx
<div className="h-screen flex flex-col bg-gray-900 text-white">
  <Header />

  <div className="flex-1 flex overflow-hidden">
    {/* Left: Conversation List Sidebar */}
    <div className="w-64 flex-shrink-0">
      {conversationListPanel}
    </div>

    {/* Middle: Chat Panel */}
    <div className="flex-1 min-w-0 border-r border-gray-700">
      {chatPanel}
    </div>

    {/* Right: Tool Panels with Tabs */}
    <div className="w-1/2 flex flex-col">
      {/* Tab Bar */}
      <div className="h-12 bg-gray-800">
        <PanelTab name="terminal" label="Terminal" icon="💻" />
        <PanelTab name="editor" label="Code Editor" icon="📝" />
        <PanelTab name="events" label="Event Stream" icon="📊" />
        <PanelTab name="browser" label="Browser" icon="🌐" />
        <PanelTab name="files" label="Files" icon="📂" />
        <PanelTab name="replay" label="Replay" icon="⏯️" />
        <PanelTab name="knowledge" label="Knowledge" icon="📚" />
        <PanelTab name="plan" label="Plan" icon="📋" />
      </div>

      {/* Active Panel Content */}
      <div className="flex-1 overflow-hidden">
        {activePanel === 'terminal' && <TerminalPanel />}
        {activePanel === 'editor' && <EditorPanel />}
        {activePanel === 'events' && <EventStreamPanel />}
        {activePanel === 'browser' && <EnhancedBrowserPanel />}
        {activePanel === 'files' && <FileTreePanel />}
        {activePanel === 'replay' && <SessionReplayPanel />}
        {activePanel === 'knowledge' && <DocumentPanel />}
        {activePanel === 'plan' && <PlanPanel />}
      </div>
    </div>
  </div>
</div>
```

### Available Panels

1. **Terminal** (💻): Shows command execution output (xterm.js)
2. **Code Editor** (📝): Monaco editor for viewing generated code
3. **Event Stream** (📊): Complete agent execution timeline
4. **Browser** (🌐): Browser automation visualization with snapshots
5. **Files** (📂): File tree and file viewer
6. **Replay** (⏯️): Session replay for debugging
7. **Knowledge** (📚): RAG knowledge base documents
8. **Plan** (📋): Agent's execution plan visualization

## State Management

**Location**: `/frontend/src/stores/agentStore.ts`

Using Zustand for global state:

```typescript
interface AgentState {
  // Multi-session support
  sessions: Session[];
  currentSessionId: string | null;

  // Connection state
  isConnected: boolean;

  // Messages
  messages: Message[];

  // Agent status
  agentStatus: 'idle' | 'thinking' | 'executing' | 'done' | 'error';
  currentIteration: number;
  maxIterations: number;

  // Terminal
  terminalOutput: TerminalOutput[];

  // Code editor
  currentCode: string;
  codeHistory: Array<{ code: string; iteration: number }>;

  // Execution context
  executionContext: ExecutionContext;

  // UI State
  activePanel: 'terminal' | 'editor' | 'browser' | 'events' | 'files' | 'replay' | 'knowledge' | 'plan';
  isSidebarOpen: boolean;

  // Actions
  handleAgentEvent: (event: AgentEvent) => void;
  addMessage: (message: Message) => void;
  setActivePanel: (panel: AgentState['activePanel']) => void;
  createNewSession: (title?: string) => Promise<string>;
  switchSession: (sessionId: string) => Promise<void>;
  // ... more actions
}
```

### Event Handling

```typescript
handleAgentEvent: (event) => {
  switch (event.type) {
    case 'status':
      set({
        agentStatus: event.content,
        currentIteration: event.metadata?.iteration,
      });
      break;

    case 'thought':
      // Add assistant message
      addMessage({
        role: 'assistant',
        content: event.content,
        timestamp: new Date(),
      });
      break;

    case 'code':
      // Update code editor and switch to editor panel
      set({ currentCode: event.content, activePanel: 'editor' });
      addCodeToHistory(event.content, event.metadata?.iteration);
      break;

    case 'output':
      // Add to terminal and switch to terminal panel
      addTerminalOutput({
        content: event.content,
        type: 'stdout',
      });
      set({ activePanel: 'terminal' });
      break;

    case 'error':
      // Show error in terminal
      addTerminalOutput({
        content: event.content,
        type: 'stderr',
      });
      set({ agentStatus: 'error', activePanel: 'terminal' });
      break;
  }
}
```

## Chat Panel Implementation

**Location**: `/frontend/src/components/Chat/ChatPanel.tsx`

```typescript
export const ChatPanel = () => {
  const { sessionId, messages, addMessage, setAgentStatus } = useAgentStore();
  const [isProcessing, setIsProcessing] = useState(false);

  const handleSendMessage = async (content: string) => {
    if (!content.trim() || isProcessing) return;

    // Add user message to UI immediately
    addMessage({
      id: `user-${Date.now()}`,
      role: 'user',
      content,
      timestamp: new Date(),
    });

    setIsProcessing(true);
    setAgentStatus('thinking');

    try {
      // Send to backend
      await apiService.chat({
        sessionId: sessionId || undefined,
        message: content,
      });

      // Real response will come via WebSocket events
    } catch (error) {
      setAgentStatus('error');
      addMessage({
        role: 'system',
        content: `Error: ${error.message}`,
      });
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="h-full flex flex-col">
      <MessageList messages={messages} />
      <ChatInput
        onSend={handleSendMessage}
        disabled={isProcessing}
        placeholder="Ask the agent to solve a task..."
      />
    </div>
  );
};
```

### Message Display

**Location**: `/frontend/src/components/Chat/MessageItem.tsx`

Features:
- Markdown rendering (react-markdown)
- Syntax highlighting for code blocks (react-syntax-highlighter)
- User/assistant/system message styling
- Timestamp display
- Expandable long messages

## Event Stream Panel

**Location**: `/frontend/src/components/EventStream/EventStreamPanel.tsx`

Displays the complete agent execution timeline following Manus AI's Event Stream Architecture.

```typescript
const EventStreamPanel: React.FC<EventStreamPanelProps> = ({
  sessionId,
  autoRefresh = false,
  refreshInterval = 2000,
}) => {
  const [events, setEvents] = useState<Event[]>([]);
  const [selectedIteration, setSelectedIteration] = useState<number | null>(null);

  // Fetch event stream
  const fetchEvents = async () => {
    const data = selectedIteration !== null
      ? await apiService.getEventsForIteration(sessionId, selectedIteration)
      : await apiService.getEventStream(sessionId);
    setEvents(data);
  };

  // Auto-refresh during execution
  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(fetchEvents, refreshInterval);
    return () => clearInterval(interval);
  }, [autoRefresh, sessionId]);

  return (
    <div className="flex flex-col h-full bg-gray-900">
      {/* Iteration filter */}
      <select value={selectedIteration ?? 'all'}>
        <option value="all">All Iterations</option>
        {iterations.map(i => <option key={i} value={i}>Iteration {i}</option>)}
      </select>

      {/* Event list */}
      <div className="flex-1 overflow-y-auto">
        {events.map(event => (
          <EventItem key={event.id} event={event} />
        ))}
      </div>
    </div>
  );
};
```

**Event Types Displayed**:
- USER_MESSAGE: User's query
- AGENT_THOUGHT: LLM's reasoning
- AGENT_ACTION: Code to execute
- OBSERVATION: Execution result (stdout, stderr, success/failure)
- AGENT_RESPONSE: Final answer
- SYSTEM: System messages
- ERROR: Errors during execution

## Terminal Panel

**Location**: `/frontend/src/components/Terminal/TerminalPanel.tsx`

Uses **xterm.js** for terminal emulation:

```typescript
import { Terminal } from '@xterm/xterm';
import '@xterm/xterm/css/xterm.css';

export const TerminalPanel = () => {
  const terminalRef = useRef<Terminal | null>(null);
  const { terminalOutput } = useAgentStore();

  useEffect(() => {
    // Initialize xterm.js
    const terminal = new Terminal({
      theme: {
        background: '#1a1a1a',
        foreground: '#f0f0f0',
      },
      fontSize: 14,
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      cursorBlink: false,
    });

    terminal.open(terminalRef.current!);
    terminalRef.current = terminal;

    return () => terminal.dispose();
  }, []);

  // Write output to terminal
  useEffect(() => {
    if (!terminalRef.current) return;

    terminalOutput.forEach(output => {
      if (output.type === 'stdout') {
        terminalRef.current.writeln(output.content);
      } else if (output.type === 'stderr') {
        terminalRef.current.writeln(`\x1b[31m${output.content}\x1b[0m`); // Red
      }
    });
  }, [terminalOutput]);

  return <div ref={terminalRef} className="h-full" />;
};
```

Features:
- ANSI color support
- Auto-scroll to bottom
- Read-only (no user input)
- Ubuntu-like appearance
- Output streaming from WebSocket

## Code Editor Panel

**Location**: `/frontend/src/components/Editor/EditorPanel.tsx`

Uses **Monaco Editor** (VS Code editor):

```typescript
import Editor from '@monaco-editor/react';

export const EditorPanel = () => {
  const { currentCode } = useAgentStore();

  return (
    <Editor
      height="100%"
      language="python"
      theme="vs-dark"
      value={currentCode}
      options={{
        readOnly: true,
        minimap: { enabled: false },
        fontSize: 14,
        lineNumbers: 'on',
        scrollBeyondLastLine: false,
      }}
    />
  );
};
```

Features:
- Syntax highlighting for Python
- Auto-detect language from code
- Read-only mode (agent-generated code)
- Dark theme (VS Code Dark)
- Line numbers and folding
- Code history tracking by iteration

## Browser Panel

**Location**: `/frontend/src/components/Browser/EnhancedBrowserPanel.tsx`

Displays browser automation results:

```typescript
export const EnhancedBrowserPanel = ({ sessionId }: { sessionId: string }) => {
  const [snapshots, setSnapshots] = useState<BrowserSnapshot[]>([]);
  const [selectedSnapshot, setSelectedSnapshot] = useState<BrowserSnapshot | null>(null);

  // Fetch browser snapshots
  useEffect(() => {
    const fetchSnapshots = async () => {
      const data = await apiService.getBrowserSnapshots(sessionId);
      setSnapshots(data);
      if (data.length > 0) {
        setSelectedSnapshot(data[data.length - 1]); // Show latest
      }
    };

    fetchSnapshots();
  }, [sessionId]);

  return (
    <div className="flex flex-col h-full">
      {/* Snapshot selector */}
      <div className="flex items-center space-x-2 p-2">
        {snapshots.map((snapshot, idx) => (
          <button
            key={snapshot.id}
            onClick={() => setSelectedSnapshot(snapshot)}
            className={selectedSnapshot?.id === snapshot.id ? 'active' : ''}
          >
            Snapshot {idx + 1}
          </button>
        ))}
      </div>

      {/* Snapshot viewer */}
      {selectedSnapshot && (
        <SnapshotViewer snapshot={selectedSnapshot} />
      )}

      {/* Tabs: Page, Console, Network */}
      <Tabs>
        <Tab label="Page">
          <img src={selectedSnapshot.screenshotUrl} alt="Page screenshot" />
        </Tab>
        <Tab label="Console">
          <ConsoleViewer logs={selectedSnapshot.consoleLogs} />
        </Tab>
        <Tab label="Network">
          <NetworkViewer requests={selectedSnapshot.networkRequests} />
        </Tab>
      </Tabs>
    </div>
  );
};
```

Features:
- Screenshot display
- Console logs viewer
- Network requests viewer
- Multiple snapshot navigation
- URL display

## File Tree Panel

**Location**: `/frontend/src/components/FileTree/FileTreePanel.tsx`

Displays workspace files with tree navigation:

```typescript
export const FileTreePanel = ({ sessionId }: { sessionId: string }) => {
  const [files, setFiles] = useState<FileNode[]>([]);
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [fileContent, setFileContent] = useState<string>('');

  // Fetch file tree
  useEffect(() => {
    const fetchFiles = async () => {
      const data = await apiService.getWorkspaceFiles(sessionId);
      setFiles(data);
    };

    fetchFiles();
  }, [sessionId]);

  // Load file content when selected
  const handleFileSelect = async (filePath: string) => {
    setSelectedFile(filePath);
    const content = await apiService.readFile(sessionId, filePath);
    setFileContent(content);
  };

  return (
    <div className="flex h-full">
      {/* File tree */}
      <div className="w-64 border-r border-gray-700 overflow-y-auto">
        {files.map(file => (
          <FileNode
            key={file.path}
            file={file}
            onSelect={handleFileSelect}
            selected={file.path === selectedFile}
          />
        ))}
      </div>

      {/* File viewer */}
      <div className="flex-1">
        <FileViewer content={fileContent} filePath={selectedFile} />
      </div>
    </div>
  );
};
```

## WebSocket Integration

**Location**: `/frontend/src/services/websocket.ts`

Using STOMP over WebSocket:

```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export class WebSocketService {
  private client: Client | null = null;
  private sessionId: string | null = null;

  connect(sessionId: string, onEvent: (event: AgentEvent) => void) {
    this.sessionId = sessionId;

    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log('WebSocket connected');

        // Subscribe to agent events
        this.client!.subscribe(
          `/topic/agent/${sessionId}`,
          (message) => {
            const event = JSON.parse(message.body);
            onEvent(event);
          }
        );
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame);
      },
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
    }
  }
}
```

**Subscription Topics**:
- `/topic/agent/{sessionId}`: Agent events (status, code, output, errors)
- `/topic/notifications/{sessionId}`: User notifications
- `/topic/browser/{sessionId}`: Browser updates

## Session Management

**Multi-Session Support**: Users can have multiple concurrent sessions.

```typescript
// Load all sessions
await loadSessions();

// Create new session
const sessionId = await createNewSession("My Task");

// Switch to existing session
await switchSession(sessionId);

// Delete session
await deleteSession(sessionId);

// Rename session
await renameSession(sessionId, "New Title");
```

Sessions are listed in the ConversationList sidebar.

## Styling

**Tailwind CSS 4** for utility-first styling:

```tsx
<div className="h-screen flex flex-col bg-gray-900 text-white">
  <div className="flex-1 overflow-hidden border-r border-gray-700">
    <div className="px-4 py-2 rounded-lg hover:bg-gray-800 transition-colors">
      {/* Content */}
    </div>
  </div>
</div>
```

**Color Scheme**:
- Background: `bg-gray-900` (#1a1a1a)
- Text: `text-white` / `text-gray-100`
- Borders: `border-gray-700`
- Hover: `hover:bg-gray-800`
- Accent: `bg-blue-500` / `bg-blue-600`
- Success: `bg-green-500`
- Error: `bg-red-500`

## Performance Optimization

### Code Splitting

```typescript
// Lazy load heavy components
const MonacoEditor = lazy(() => import('@monaco-editor/react'));
const Terminal = lazy(() => import('@xterm/xterm'));

// Use with Suspense
<Suspense fallback={<LoadingSpinner />}>
  <MonacoEditor />
</Suspense>
```

### Memoization

```typescript
const MessageItem = memo(({ message }: MessageItemProps) => {
  // Component only re-renders when message changes
  return <div>{message.content}</div>;
});
```

### Virtual Scrolling

For long event streams or message lists (future enhancement).

## Testing

**Location**: `/frontend/src/components/__tests__/`

Using Vitest for unit and component tests:

```typescript
import { render, screen } from '@testing-library/react';
import { ChatPanel } from '../Chat/ChatPanel';

describe('ChatPanel', () => {
  it('renders message list and input', () => {
    render(<ChatPanel />);
    expect(screen.getByPlaceholderText(/ask the agent/i)).toBeInTheDocument();
  });

  it('sends message on submit', async () => {
    const { user } = setup(<ChatPanel />);
    const input = screen.getByRole('textbox');

    await user.type(input, 'Test message');
    await user.keyboard('{Enter}');

    expect(apiService.chat).toHaveBeenCalledWith({
      message: 'Test message',
    });
  });
});
```

**Test Coverage**:
- Component rendering
- User interactions
- State updates
- WebSocket event handling
- API calls
- Error scenarios

## Build Configuration

**Location**: `/frontend/vite.config.ts`

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
});
```

## Key Features Summary

1. **Three-Panel Layout**: Conversation list, chat, and tool panels
2. **8 Tool Panels**: Terminal, Editor, Events, Browser, Files, Replay, Knowledge, Plan
3. **Event Stream**: Complete transparency into agent execution
4. **Multi-Session**: Manage multiple conversations
5. **Real-Time Updates**: WebSocket for live agent events
6. **Code Editor**: Monaco editor with syntax highlighting
7. **Terminal**: xterm.js for command output
8. **Browser Visualization**: Screenshots, console, network logs
9. **File Management**: Tree view and file viewer
10. **Session Replay**: Debug and review past executions

## Best Practices

1. **Use Zustand actions** for all state mutations
2. **Memoize expensive components** to prevent re-renders
3. **Lazy load heavy components** (Monaco, xterm)
4. **Handle WebSocket disconnections** gracefully
5. **Show loading states** for all async operations
6. **Test components** with realistic data
7. **Use TypeScript** strictly for type safety
8. **Follow Tailwind conventions** for consistent styling
9. **Implement error boundaries** for graceful failures
10. **Profile performance** regularly during development

## Common Issues

### Monaco Editor Not Loading
**Solution**: Use dynamic import and show loading placeholder

### WebSocket Connection Drops
**Solution**: Implement auto-reconnect with exponential backoff

### Terminal Performance with Large Output
**Solution**: Limit terminal buffer size, use virtual scrolling

### State Synchronization Bugs
**Solution**: Use Zustand DevTools for debugging state changes

## Development Workflow

1. **Start backend**: `cd backend && ./mvnw spring-boot:run`
2. **Start frontend**: `cd frontend && npm run dev`
3. **Open browser**: `http://localhost:3000`
4. **WebSocket connects automatically**
5. **Create session and start chatting**
6. **Watch real-time updates** in tool panels
