export interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
}

export interface AgentEvent {
  type: 'status' | 'thought' | 'code' | 'output' | 'error' | 'warning' | 'connected';
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
