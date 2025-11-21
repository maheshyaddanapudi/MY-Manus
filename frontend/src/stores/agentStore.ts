import { create } from 'zustand';
import { Message, AgentEvent, TerminalOutput, ExecutionContext } from '../types';

interface AgentState {
  // Session
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
  activePanel: 'terminal' | 'editor' | 'browser';
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
}

export const useAgentStore = create<AgentState>((set, get) => ({
  // Initial state
  sessionId: null,
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
}));
