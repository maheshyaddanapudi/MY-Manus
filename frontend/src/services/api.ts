import axios from 'axios';
import type { AxiosInstance } from 'axios';
import type { ChatRequest, ChatResponse, SessionStatus, Message, ExecutionContext, Event } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class ApiService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 300000, // 5 minutes for long-running agent tasks
    });
  }

  // Health check
  async health(): Promise<any> {
    const response = await this.client.get('/api/health');
    return response.data;
  }

  // Chat with agent
  async chat(request: ChatRequest): Promise<ChatResponse> {
    const response = await this.client.post<ChatResponse>('/api/agent/chat', request);
    return response.data;
  }

  // Session management
  async createSession(sessionId?: string, title?: string): Promise<{ sessionId: string; message: string }> {
    const params: any = {};
    if (sessionId) params.sessionId = sessionId;
    if (title) params.title = title;

    const response = await this.client.post('/api/agent/session', null, {
      params: Object.keys(params).length > 0 ? params : undefined,
    });
    return response.data;
  }

  async listSessions(): Promise<Array<{
    sessionId: string;
    title: string;
    createdAt: string;
    updatedAt: string;
    messageCount: number;
  }>> {
    const response = await this.client.get('/api/agent/sessions');
    return response.data;
  }

  async getSessionStatus(sessionId: string): Promise<SessionStatus> {
    const response = await this.client.get<SessionStatus>(`/api/agent/session/${sessionId}`);
    return response.data;
  }

  async updateSessionTitle(sessionId: string, title: string): Promise<{ message: string; sessionId: string; title: string }> {
    const response = await this.client.put(`/api/agent/session/${sessionId}/title`, null, {
      params: { title },
    });
    return response.data;
  }

  async clearSession(sessionId: string): Promise<{ message: string; sessionId: string }> {
    const response = await this.client.delete(`/api/agent/session/${sessionId}`);
    return response.data;
  }

  // Conversation history
  async getMessages(sessionId: string): Promise<Message[]> {
    const response = await this.client.get<Message[]>(`/api/agent/session/${sessionId}/messages`);
    return response.data;
  }

  // Execution context
  async getExecutionContext(sessionId: string): Promise<ExecutionContext> {
    const response = await this.client.get<ExecutionContext>(
      `/api/agent/session/${sessionId}/context`
    );
    return response.data;
  }

  // Tool executions
  async getToolExecutions(sessionId: string): Promise<any[]> {
    const response = await this.client.get(`/api/agent/session/${sessionId}/tools`);
    return response.data;
  }

  // Event stream
  async getEventStream(sessionId: string): Promise<Event[]> {
    const response = await this.client.get<Event[]>(`/api/agent/session/${sessionId}/events`);
    return response.data;
  }

  async getEventsForIteration(sessionId: string, iteration: number): Promise<Event[]> {
    const response = await this.client.get<Event[]>(
      `/api/agent/session/${sessionId}/events/iteration/${iteration}`
    );
    return response.data;
  }

  // File browsing (read-only, extensible for editing later)
  async listFiles(path: string = '/workspace', maxDepth: number = 3, includeHidden: boolean = false): Promise<any> {
    const response = await this.client.get('/api/files/list', {
      params: { path, maxDepth, includeHidden },
    });
    return response.data;
  }

  async readFile(path: string): Promise<any> {
    const response = await this.client.get('/api/files/read', {
      params: { path },
    });
    return response.data;
  }

  // FUTURE: File editing methods (uncomment when backend implements them)
  /*
  async writeFile(path: string, content: string): Promise<any> {
    const response = await this.client.post('/api/files/write', content, {
      params: { path },
      headers: { 'Content-Type': 'text/plain' },
    });
    return response.data;
  }

  async deleteFile(path: string): Promise<any> {
    const response = await this.client.delete('/api/files/delete', {
      params: { path },
    });
    return response.data;
  }

  async createFile(path: string, type: 'file' | 'directory' = 'file'): Promise<any> {
    const response = await this.client.post('/api/files/create', null, {
      params: { path, type },
    });
    return response.data;
  }
  */
}

export const apiService = new ApiService();

// Note: Once backend is running, generate typed client with:
// npm run generate-api
// This will create a fully typed client from the OpenAPI spec at:
// http://localhost:8080/v3/api-docs
