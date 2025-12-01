import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

// Mock axios BEFORE importing apiService
vi.mock('axios', () => {
  const mockAxiosInstance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  };
  
  return {
    default: {
      create: vi.fn(() => mockAxiosInstance),
    },
  };
});

import { apiService } from '../api';

// Get the mocked axios instance for assertions
const mockAxiosInstance = (axios.create as any)();

describe('API Service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends message to agent', async () => {
    const mockResponse = { 
      sessionId: 'session-1',
      response: 'Hello back!',
      status: 'completed'
    };

    mockAxiosInstance.post.mockResolvedValueOnce({
      data: mockResponse,
    });

    const result = await apiService.chat({
      sessionId: 'session-1',
      message: 'Hello',
    });

    expect(mockAxiosInstance.post).toHaveBeenCalledWith(
      '/api/agent/chat',
      expect.objectContaining({
        sessionId: 'session-1',
        message: 'Hello',
      })
    );

    expect(result).toEqual(mockResponse);
  });

  it('fetches event stream', async () => {
    const mockEvents = [
      { 
        id: 'event-1',
        type: 'ITERATION_START',
        iteration: 1,
        content: 'Starting iteration',
        timestamp: new Date().toISOString()
      },
    ];

    mockAxiosInstance.get.mockResolvedValueOnce({
      data: mockEvents,
    });

    const events = await apiService.getEventStream('session-1');

    expect(mockAxiosInstance.get).toHaveBeenCalledWith(
      '/api/agent/session/session-1/events'
    );

    expect(events).toEqual(mockEvents);
  });

  it('fetches messages', async () => {
    const mockMessages = [
      { 
        id: 'msg-1',
        role: 'USER',
        content: 'Hello',
        timestamp: new Date()
      },
    ];

    mockAxiosInstance.get.mockResolvedValueOnce({
      data: mockMessages,
    });

    const messages = await apiService.getMessages('session-1');

    expect(mockAxiosInstance.get).toHaveBeenCalledWith(
      '/api/agent/session/session-1/messages'
    );

    expect(messages).toEqual(mockMessages);
  });

  it('fetches sessions', async () => {
    const mockSessions = [
      { 
        sessionId: 'session-1',
        title: 'Conversation 1',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        messageCount: 5
      },
    ];

    mockAxiosInstance.get.mockResolvedValueOnce({
      data: mockSessions,
    });

    const sessions = await apiService.listSessions();

    expect(mockAxiosInstance.get).toHaveBeenCalledWith(
      '/api/agent/sessions'
    );

    expect(sessions).toEqual(mockSessions);
  });

  it('creates new session', async () => {
    const mockSession = { 
      sessionId: 'new-session',
      message: 'Session created successfully'
    };

    mockAxiosInstance.post.mockResolvedValueOnce({
      data: mockSession,
    });

    const session = await apiService.createSession(undefined, 'New Conversation');

    expect(mockAxiosInstance.post).toHaveBeenCalledWith(
      '/api/agent/session',
      null,
      expect.objectContaining({
        params: expect.objectContaining({
          title: 'New Conversation'
        })
      })
    );

    expect(session).toEqual(mockSession);
  });

  it('deletes session', async () => {
    const mockResponse = { 
      message: 'Session deleted successfully',
      sessionId: 'session-1'
    };

    mockAxiosInstance.delete.mockResolvedValueOnce({
      data: mockResponse,
    });

    const result = await apiService.clearSession('session-1');

    expect(mockAxiosInstance.delete).toHaveBeenCalledWith(
      '/api/agent/session/session-1'
    );

    expect(result).toEqual(mockResponse);
  });

  it('fetches tool executions', async () => {
    const mockExecutions = [
      { 
        id: 1,
        toolName: 'file_read',
        result: { content: 'file contents' }
      },
    ];

    mockAxiosInstance.get.mockResolvedValueOnce({
      data: mockExecutions,
    });

    const executions = await apiService.getToolExecutions('session-1');

    expect(mockAxiosInstance.get).toHaveBeenCalledWith(
      '/api/agent/session/session-1/tools'
    );

    expect(executions).toEqual(mockExecutions);
  });

  it('handles API errors', async () => {
    const errorResponse = {
      response: {
        status: 500,
        statusText: 'Internal Server Error',
        data: { error: 'Something went wrong' }
      }
    };

    mockAxiosInstance.post.mockRejectedValueOnce(errorResponse);

    await expect(
      apiService.chat({
        sessionId: 'session-1',
        message: 'Hello',
      })
    ).rejects.toMatchObject({
      response: expect.objectContaining({
        status: 500,
      })
    });
  });
});
