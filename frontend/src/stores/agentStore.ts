import { create } from 'zustand';
import type { Message, AgentEvent, TerminalOutput, ExecutionContext, Session } from '../types';
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
  setSessionId: (sessionId: string) => void;
  setConnected: (connected: boolean) => void;
  addMessage: (message: Message) => void;
  setMessages: (messages: Message[]) => void;
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
  agentStatus: 'idle',
  currentIteration: 0,
  maxIterations: 20,
  terminalOutput: [],
  currentCode: '',
  codeHistory: [],
  executionContext: {},
  activePanel: 'terminal',
  isSidebarOpen: true,

  // Actions
  setSessionId: (sessionId) => set({ sessionId }),

  setConnected: (connected) => set({ isConnected: connected }),

  addMessage: (message) => set((state) => ({
    messages: [...state.messages, message],
  })),

  setMessages: (messages) => set({ messages }),

  handleAgentEvent: (event) => {
    const state = get();

    switch (event.type) {
      case 'status':
        const status = event.content as AgentState['agentStatus'];
        set({
          agentStatus: status,
          currentIteration: event.metadata?.iteration || state.currentIteration,
          maxIterations: event.metadata?.maxIterations || state.maxIterations,
        });
        break;

      case 'thought':
        // Add assistant message with the thought
        get().addMessage({
          id: `thought-${Date.now()}`,
          role: 'assistant',
          content: event.content,
          timestamp: new Date(),
        });
        break;

      case 'code':
        // Update current code and add to history
        set({ currentCode: event.content, activePanel: 'editor' });
        get().addCodeToHistory(
          event.content,
          event.metadata?.iteration || state.currentIteration
        );
        break;

      case 'output':
        // Add to terminal output
        get().addTerminalOutput({
          content: event.content,
          type: 'stdout',
          timestamp: new Date(),
        });
        set({ activePanel: 'terminal' });
        break;

      case 'error':
        // Add error to terminal
        get().addTerminalOutput({
          content: event.content,
          type: 'stderr',
          timestamp: new Date(),
        });
        set({ agentStatus: 'error', activePanel: 'terminal' });
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
    agentStatus: 'idle',
    currentIteration: 0,
    terminalOutput: [],
    currentCode: '',
    codeHistory: [],
    executionContext: {},
  }),

  // Multi-session actions
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
