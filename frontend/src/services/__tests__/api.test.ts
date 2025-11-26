import { describe, it, expect, vi, beforeEach } from 'vitest';
import * as apiService from '../api';

// Mock fetch
global.fetch = vi.fn();

describe('API Service', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends message to agent', async () => {
    const mockResponse = { success: true };

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    });

    const result = await apiService.sendMessage('session-1', 'Hello');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/agent/message'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('Hello'),
      })
    );

    expect(result).toEqual(mockResponse);
  });

  it('fetches event stream', async () => {
    const mockEvents = [
      { id: 1, type: 'USER_MESSAGE', data: { text: 'Hello' } },
    ];

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockEvents,
    });

    const events = await apiService.getEventStream('session-1');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/agent/events/session-1')
    );

    expect(events).toEqual(mockEvents);
  });

  it('fetches messages', async () => {
    const mockMessages = [
      { id: 1, role: 'USER', content: 'Hello' },
    ];

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockMessages,
    });

    const messages = await apiService.getMessages('session-1');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/agent/messages/session-1')
    );

    expect(messages).toEqual(mockMessages);
  });

  it('fetches sessions', async () => {
    const mockSessions = [
      { sessionId: 'session-1', title: 'Conversation 1' },
    ];

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockSessions,
    });

    const sessions = await apiService.getSessions();

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/agent/sessions')
    );

    expect(sessions).toEqual(mockSessions);
  });

  it('creates new session', async () => {
    const mockSession = { sessionId: 'new-session', title: 'New Conversation' };

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockSession,
    });

    const session = await apiService.createSession('New Conversation');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/agent/sessions'),
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('New Conversation'),
      })
    );

    expect(session).toEqual(mockSession);
  });

  it('deletes session', async () => {
    const mockResponse = { success: true };

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    });

    const result = await apiService.deleteSession('session-1');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/agent/sessions/session-1'),
      expect.objectContaining({ method: 'DELETE' })
    );

    expect(result).toEqual(mockResponse);
  });

  it('fetches tool executions', async () => {
    const mockExecutions = [
      { id: 1, toolName: 'file_read', result: {} },
    ];

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockExecutions,
    });

    const executions = await apiService.getToolExecutions('session-1');

    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/agent/tool-executions/session-1')
    );

    expect(executions).toEqual(mockExecutions);
  });

  it('handles API errors', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    });

    await expect(apiService.sendMessage('session-1', 'Hello')).rejects.toThrow();
  });
});
