import { create } from 'zustand';
import type { Message, AgentEvent, TerminalOutput, ExecutionContext, Session, Event, EventType } from '../types';
import { apiService } from '../services/api';

interface AgentState {
  // Multi-session support
  sessions: Session[];
  currentSessionId: string | null;

  // Legacy (keep for backward compatibility)
  sessionId: string | null;
  isConnected: boolean;

  // Messages
  messages: Message[];

  // Events
  events: Event[];

  // Agent status
  agentStatus: 'idle' | 'thinking' | 'executing' | 'done' | 'error' | 'stopping' | 'stopped';
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
  suggestedPanel: 'terminal' | 'editor' | 'browser' | 'events' | 'files' | 'replay' | 'knowledge' | 'plan' | null;
  isSidebarOpen: boolean;

  // Chunk buffering for streaming events
  thoughtBuffer: string;
  lastThoughtMessageId: string | null;
  lastEventId: string | null;
  messageBuffer: string;
  lastMessageId: string | null;

  // Actions
  setSessionId: (sessionId: string) => void;
  setConnected: (connected: boolean) => void;
  addMessage: (message: Message) => void;
  setMessages: (messages: Message[]) => void;
  addEvent: (event: Event) => void;
  setEvents: (events: Event[]) => void;
  handleAgentEvent: (event: AgentEvent) => void;
  addTerminalOutput: (output: TerminalOutput) => void;
  clearTerminal: () => void;
  setCurrentCode: (code: string) => void;
  addCodeToHistory: (code: string, iteration: number) => void;
  setExecutionContext: (context: ExecutionContext) => void;
  setAgentStatus: (status: AgentState['agentStatus']) => void;
  setActivePanel: (panel: AgentState['activePanel']) => void;
  toggleSidebar: () => void;
  resetSession: () => void;

  // Multi-session actions
  setCurrentSession: (sessionId: string | null) => void;
  addSession: (session: Session) => void;
  setSessions: (sessions: Session[]) => void;
  clearSession: () => void;
  loadSessions: () => Promise<void>;
  createNewSession: (title?: string) => Promise<string>;
  switchSession: (sessionId: string) => Promise<void>;
  deleteSession: (sessionId: string) => Promise<void>;
  renameSession: (sessionId: string, title: string) => Promise<void>;
}

export const useAgentStore = create<AgentState>((set, get) => ({
  // Initial state
  sessions: [],
  currentSessionId: null,
  sessionId: null, // Legacy
  isConnected: false,
  messages: [],
  events: [],
  agentStatus: 'idle',
  currentIteration: 0,
  maxIterations: 20,
  terminalOutput: [],
  currentCode: '',
  codeHistory: [],
  executionContext: {},
  activePanel: 'terminal',
  suggestedPanel: null,
  isSidebarOpen: true,
      thoughtBuffer: '',
      lastThoughtMessageId: null,
      lastEventId: null,
      messageBuffer: '',
      lastMessageId: null,

  // Actions
  setSessionId: (sessionId) => set({ sessionId }),

  setConnected: (connected) => set({ isConnected: connected }),

  addMessage: (message) => set((state) => ({
    messages: [...state.messages, message],
  })),

  setMessages: (messages) => set({ messages }),

  addEvent: (event) => set((state) => ({
    events: [...state.events, event],
  })),

  setEvents: (events) => set({ events }),

  handleAgentEvent: (event) => {
    const state = get();

    // Handle message_chunk - accumulate final summary message
    if (event.type === 'message_chunk') {
      const newBuffer = state.messageBuffer + event.content;
      
      // Update or create message
      if (state.lastMessageId) {
        set((state) => ({
          messages: state.messages.map(m => 
            m.id === state.lastMessageId
              ? { ...m, content: newBuffer }
              : m
          ),
          messageBuffer: newBuffer,
        }));
      } else {
        const messageId = `message-${Date.now()}`;
        get().addMessage({
          id: messageId,
          role: 'assistant',
          content: newBuffer,
          timestamp: new Date(),
          sourceType: 'message',
        });
        set({ lastMessageId: messageId, messageBuffer: newBuffer });
      }
      
      return; // Don't process further
    }
    
    // Handle complete message
    if (event.type === 'message') {
      const finalContent = event.content;
      
      // Finalize message
      if (state.lastMessageId) {
        set((state) => ({
          messages: state.messages.map(m => 
            m.id === state.lastMessageId
              ? { ...m, content: finalContent }
              : m
          ),
          messageBuffer: '',
          lastMessageId: null,
        }));
      } else {
        // No chunks, just add the complete message
        get().addMessage({
          id: `message-${Date.now()}`,
          role: 'assistant',
          content: finalContent,
          timestamp: new Date(),
          sourceType: 'message',
        });
      }
      
      return; // Don't process further
    }
    
    // Handle output - attach to last message as observation
    if (event.type === 'output') {
      const output = event.content;
      
      // Find the last assistant message and append observation
      set((state) => {
        const messages = [...state.messages];
        for (let i = messages.length - 1; i >= 0; i--) {
          if (messages[i].role === 'assistant') {
            // Append observation to the message content
            const currentContent = messages[i].content;
            // Check if there's an execute block to attach to
            if (currentContent.includes('<execute>')) {
              // Find the last </execute> and insert observation after it
              const lastExecuteIndex = currentContent.lastIndexOf('</execute>');
              if (lastExecuteIndex !== -1) {
                const before = currentContent.substring(0, lastExecuteIndex + 10); // +10 for </execute>
                const after = currentContent.substring(lastExecuteIndex + 10);
                messages[i] = {
                  ...messages[i],
                  content: before + `\n<observation>${output}</observation>` + after
                };
              }
            }
            break;
          }
        }
        return { messages };
      });
      
      // Don't return - let it continue to add to events
    }
    
    // Handle thought chunks - merge them into continuous events and messages
    if (event.type === 'thought_chunk') {
      const newBuffer = state.thoughtBuffer + event.content;
      
      // Update or create event in Event Stream
      if (state.lastEventId) {
        // Update existing event
        set((state) => ({
          events: state.events.map(e => 
            e.id === state.lastEventId 
              ? { ...e, content: newBuffer }
              : e
          ),
          thoughtBuffer: newBuffer,
        }));
      } else {
        // Create new event
        const eventId = `thought-${event.metadata?.iteration || state.currentIteration}-${Date.now()}`;
        const eventRecord: Event = {
          id: eventId,
          type: 'AGENT_THOUGHT',
          content: newBuffer,
          timestamp: new Date().toISOString(),
          iteration: event.metadata?.iteration || state.currentIteration,
          sequence: state.events.length,
          data: event.metadata || {},
        };
        get().addEvent(eventRecord);
        set({ lastEventId: eventId });
      }
      
      // Update or create message in chat
      if (state.lastThoughtMessageId) {
        // Update existing message
        set((state) => ({
          messages: state.messages.map(m => 
            m.id === state.lastThoughtMessageId
              ? { ...m, content: newBuffer }
              : m
          ),
        }));
      } else {
        // Create new message
        const messageId = `thought-${Date.now()}`;
        get().addMessage({
          id: messageId,
          role: 'assistant',
          content: newBuffer,
          timestamp: new Date(),
          sourceType: 'thought',
        });
        set({ lastThoughtMessageId: messageId });
      }
      
      return; // Don't process further
    }
    
    // Handle complete thought - only finalize if complete flag is set
    if (event.type === 'thought') {
      const isComplete = event.metadata?.complete === true;
      const isStreaming = event.metadata?.streaming === true;
      
      // If streaming (not complete), treat like a chunk
      if (isStreaming && !isComplete) {
        const newBuffer = state.thoughtBuffer + event.content;
        
        // Update or create event
        if (state.lastEventId) {
          set((state) => ({
            events: state.events.map(e => 
              e.id === state.lastEventId 
                ? { ...e, content: newBuffer }
                : e
            ),
            thoughtBuffer: newBuffer,
          }));
        } else {
          const eventId = `thought-${event.metadata?.iteration || state.currentIteration}-${Date.now()}`;
          const eventRecord: Event = {
            id: eventId,
            type: 'AGENT_THOUGHT',
            content: newBuffer,
            timestamp: new Date().toISOString(),
            iteration: event.metadata?.iteration || state.currentIteration,
            sequence: state.events.length,
            data: event.metadata || {},
          };
          get().addEvent(eventRecord);
          set({ lastEventId: eventId });
        }
        
        // Update or create message
        if (state.lastThoughtMessageId) {
          set((state) => ({
            messages: state.messages.map(m => 
              m.id === state.lastThoughtMessageId
                ? { ...m, content: newBuffer }
                : m
            ),
          }));
        } else {
          const messageId = `thought-${Date.now()}`;
          get().addMessage({
            id: messageId,
            role: 'assistant',
            content: newBuffer,
            timestamp: new Date(),
          });
          set({ lastThoughtMessageId: messageId });
        }
        
        return; // Don't process further
      }
      
      // Complete thought - finalize
      if (isComplete) {
        // Use the complete content from the event (not buffer)
        const finalContent = event.content;
        
        // Finalize event
        if (state.lastEventId) {
          set((state) => ({
            events: state.events.map(e => 
              e.id === state.lastEventId 
                ? { ...e, content: finalContent }
                : e
            ),
            thoughtBuffer: '',
            lastEventId: null,
          }));
        } else {
          // No chunks, just add the complete thought
          const eventRecord: Event = {
            id: `thought-${event.metadata?.iteration || state.currentIteration}-${Date.now()}`,
            type: 'AGENT_THOUGHT',
            content: finalContent,
            timestamp: new Date().toISOString(),
            iteration: event.metadata?.iteration || state.currentIteration,
            sequence: state.events.length,
            data: event.metadata || {},
          };
          get().addEvent(eventRecord);
        }
        
        // Finalize message
        if (state.lastThoughtMessageId) {
          set((state) => ({
            messages: state.messages.map(m => 
              m.id === state.lastThoughtMessageId
                ? { ...m, content: finalContent }
                : m
            ),
            thoughtBuffer: '',
            lastThoughtMessageId: null,
          }));
        } else {
          // No chunks, just add the complete thought
          get().addMessage({
            id: `thought-${Date.now()}`,
            role: 'assistant',
            content: finalContent,
            timestamp: new Date(),
          });
        }
        
        return; // Don't process further
      }
    }

    // For other event types, add to events array
    const typeMap: Record<string, EventType> = {
      'code': 'AGENT_ACTION',
      'output': 'OBSERVATION',
      'error': 'ERROR',
      'status': 'SYSTEM',
      'connected': 'SYSTEM',
    };
    
    const eventType = typeMap[event.type] || 'SYSTEM';
    if (eventType !== 'SYSTEM' || event.type === 'status') {
      const eventRecord: Event = {
        id: `${event.type}-${Date.now()}-${Math.random()}`,
        type: eventType,
        content: event.content,
        timestamp: new Date().toISOString(),
        iteration: event.metadata?.iteration || state.currentIteration,
        sequence: state.events.length,
        data: event.metadata || {},
      };
      get().addEvent(eventRecord);
    }

    switch (event.type) {
      case 'status':
        const status = event.content as AgentState['agentStatus'];
        set({
          agentStatus: status,
          currentIteration: event.metadata?.iteration || state.currentIteration,
          maxIterations: event.metadata?.maxIterations || state.maxIterations,
        });
        break;

      case 'code':
        // Update current code and add to history
        // Suggest editor panel but don't auto-switch
        set({ currentCode: event.content, suggestedPanel: 'editor' });
        get().addCodeToHistory(
          event.content,
          event.metadata?.iteration || state.currentIteration
        );
        break;

      case 'output':
        // Add to terminal output
        // Suggest terminal panel but don't auto-switch
        get().addTerminalOutput({
          content: event.content,
          type: 'stdout',
          timestamp: new Date(),
        });
        set({ suggestedPanel: 'terminal' });
        break;

      case 'error':
        // Add error to terminal
        // Suggest terminal panel but don't auto-switch
        get().addTerminalOutput({
          content: event.content,
          type: 'stderr',
          timestamp: new Date(),
        });
        set({ agentStatus: 'error', suggestedPanel: 'terminal' });
        break;

      case 'connected':
        set({ isConnected: true });
        break;
    }
  },

  addTerminalOutput: (output) => set((state) => ({
    terminalOutput: [...state.terminalOutput, output],
  })),

  clearTerminal: () => set({ terminalOutput: [] }),

  setCurrentCode: (code) => set({ currentCode: code }),

  addCodeToHistory: (code, iteration) => set((state) => ({
    codeHistory: [...state.codeHistory, { code, iteration }],
  })),

  setExecutionContext: (context) => set({ executionContext: context }),

  setAgentStatus: (status) => set({ agentStatus: status }),

  setActivePanel: (panel) => set({ activePanel: panel }),

  toggleSidebar: () => set((state) => ({ isSidebarOpen: !state.isSidebarOpen })),

  resetSession: () => set({
    messages: [],
    // Don't clear events - they should persist for Event Stream tab
    agentStatus: 'idle',
    currentIteration: 0,
    terminalOutput: [],
    currentCode: '',
    codeHistory: [],
    executionContext: {},
  }),

  // Multi-session actions
  setCurrentSession: (sessionId) => set({ currentSessionId: sessionId, sessionId }),

  addSession: (session) => set((state) => ({
    sessions: [...state.sessions, session],
  })),

  setSessions: (sessions) => set({ sessions }),

  clearSession: () => set({
    currentSessionId: null,
    sessionId: null,
    // Don't clear events - they should persist for Event Stream tab
    messages: [],
    terminalOutput: [],
    currentCode: '',
    codeHistory: [],
    executionContext: {},
    agentStatus: 'idle',
    currentIteration: 0,
  }),

  loadSessions: async () => {
    try {
      const sessions = await apiService.listSessions();
      set({ sessions });
    } catch (error) {
      console.error('Failed to load sessions:', error);
    }
  },

  createNewSession: async (title?: string) => {
    try {
      const response = await apiService.createSession(undefined, title);
      const sessionId = response.sessionId;

      // Reload sessions list
      await get().loadSessions();

      // Switch to new session
      await get().switchSession(sessionId);

      return sessionId;
    } catch (error) {
      console.error('Failed to create session:', error);
      throw error;
    }
  },

  switchSession: async (sessionId: string) => {
    try {
      // Clear current session state
      get().resetSession();

      // Set new session ID
      set({
        sessionId,
        currentSessionId: sessionId,
        agentStatus: 'idle',
      });

      // Load messages for this session
      const messages = await apiService.getMessages(sessionId);
      set({
        messages: messages.map((msg) => ({
          ...msg,
          timestamp: new Date(msg.timestamp),
        })),
      });

      // Load execution context
      const context = await apiService.getExecutionContext(sessionId);
      set({ executionContext: context });

      // Load events for this session
      const events = await apiService.getEventStream(sessionId);
      set({ events });

      console.log(`Switched to session: ${sessionId}`);
    } catch (error) {
      console.error('Failed to switch session:', error);
      // Session might not exist yet, that's okay
    }
  },

  deleteSession: async (sessionId: string) => {
    try {
      await apiService.clearSession(sessionId);

      // Remove from sessions list
      set((state) => ({
        sessions: state.sessions.filter((s) => s.sessionId !== sessionId),
      }));

      // If deleted current session, create a new one
      if (get().currentSessionId === sessionId) {
        await get().createNewSession();
      }
    } catch (error) {
      console.error('Failed to delete session:', error);
      throw error;
    }
  },

  renameSession: async (sessionId: string, title: string) => {
    try {
      await apiService.updateSessionTitle(sessionId, title);

      // Update in sessions list
      set((state) => ({
        sessions: state.sessions.map((s) =>
          s.sessionId === sessionId ? { ...s, title } : s
        ),
      }));
    } catch (error) {
      console.error('Failed to rename session:', error);
      throw error;
    }
  },
}));
