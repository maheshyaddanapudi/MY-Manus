import axios, { AxiosInstance } from 'axios';
import { ChatRequest, ChatResponse, SessionStatus, Message, ExecutionContext } from '../types';

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
  async createSession(sessionId?: string): Promise<{ sessionId: string; message: string }> {
    const response = await this.client.post('/api/agent/session', null, {
      params: sessionId ? { sessionId } : undefined,
    });
    return response.data;
  }

  async getSessionStatus(sessionId: string): Promise<SessionStatus> {
    const response = await this.client.get<SessionStatus>(`/api/agent/session/${sessionId}`);
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
}

export const apiService = new ApiService();

// Note: Once backend is running, generate typed client with:
// npm run generate-api
// This will create a fully typed client from the OpenAPI spec at:
// http://localhost:8080/v3/api-docs
