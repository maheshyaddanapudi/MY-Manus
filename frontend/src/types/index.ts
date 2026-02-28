export interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
  sourceType?: 'thought' | 'message'; // Track if this came from thought or message event
  isStreaming?: boolean; // True while chunks are still being received
}

export interface AgentEvent {
  type: 'status' | 'thought' | 'thought_chunk' | 'message' | 'message_chunk' | 'code' | 'output' | 'error' | 'warning' | 'connected';
  content: string;
  metadata?: Record<string, any>;
}

export interface ChatRequest {
  sessionId?: string;
  message: string;
}

export interface ChatResponse {
  sessionId: string;
  message: string;
  status: 'pending' | 'completed' | 'error';
}

export interface Session {
  sessionId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

export interface SessionStatus {
  sessionId: string;
  exists: boolean;
  messageCount?: number;
  context?: string[];
  metadata?: Record<string, any>;
}

export interface ExecutionContext {
  [key: string]: any;
}

export interface CodeExecution {
  code: string;
  iteration: number;
  block: number;
  total: number;
}

export interface TerminalOutput {
  content: string;
  type: 'stdout' | 'stderr';
  timestamp: Date;
}

export type EventType =
  | 'USER_MESSAGE'
  | 'AGENT_THOUGHT'
  | 'AGENT_ACTION'
  | 'OBSERVATION'
  | 'AGENT_RESPONSE'
  | 'SYSTEM'
  | 'ERROR';

export interface Event {
  id: string;
  type: EventType;
  iteration: number;
  sequence: number;
  content: string;
  data?: Record<string, any>;
  timestamp: string;
  durationMs?: number;
  success?: boolean;
  error?: string;
}
