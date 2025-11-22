import { describe, it, expect, beforeEach } from 'vitest';
import { useAgentStore } from '../agentStore';

describe('agentStore', () => {
  beforeEach(() => {
    // Reset store before each test
    useAgentStore.setState({
      currentSessionId: null,
      sessions: [],
      events: [],
      messages: [],
    });
  });

  it('initializes with default state', () => {
    const state = useAgentStore.getState();
    expect(state.currentSessionId).toBeNull();
    expect(state.sessions).toEqual([]);
    expect(state.events).toEqual([]);
    expect(state.messages).toEqual([]);
  });

  it('sets current session', () => {
    useAgentStore.getState().setCurrentSession('session-123');
    expect(useAgentStore.getState().currentSessionId).toBe('session-123');
  });

  it('adds session', () => {
    const session = {
      sessionId: 'session-1',
      title: 'Test Session',
      createdAt: new Date(),
      updatedAt: new Date(),
      active: true,
    };

    useAgentStore.getState().addSession(session);
    expect(useAgentStore.getState().sessions).toHaveLength(1);
    expect(useAgentStore.getState().sessions[0]).toEqual(session);
  });

  it('updates sessions list', () => {
    const sessions = [
      { sessionId: 'session-1', title: 'Session 1', createdAt: new Date(), updatedAt: new Date(), active: true },
      { sessionId: 'session-2', title: 'Session 2', createdAt: new Date(), updatedAt: new Date(), active: true },
    ];

    useAgentStore.getState().setSessions(sessions);
    expect(useAgentStore.getState().sessions).toHaveLength(2);
  });

  it('adds event to store', () => {
    const event = {
      id: 1,
      sessionId: 'session-1',
      type: 'USER_MESSAGE' as const,
      sequence: 1,
      data: { text: 'Hello' },
      timestamp: new Date(),
    };

    useAgentStore.getState().addEvent(event);
    expect(useAgentStore.getState().events).toHaveLength(1);
    expect(useAgentStore.getState().events[0]).toEqual(event);
  });

  it('sets events for session', () => {
    const events = [
      { id: 1, sessionId: 'session-1', type: 'USER_MESSAGE' as const, sequence: 1, data: {}, timestamp: new Date() },
      { id: 2, sessionId: 'session-1', type: 'AGENT_THOUGHT' as const, sequence: 2, data: {}, timestamp: new Date() },
    ];

    useAgentStore.getState().setEvents(events);
    expect(useAgentStore.getState().events).toHaveLength(2);
  });

  it('adds message to store', () => {
    const message = {
      id: 1,
      sessionId: 'session-1',
      role: 'USER' as const,
      content: 'Test message',
      createdAt: new Date(),
    };

    useAgentStore.getState().addMessage(message);
    expect(useAgentStore.getState().messages).toHaveLength(1);
    expect(useAgentStore.getState().messages[0]).toEqual(message);
  });

  it('clears session data', () => {
    // Add some data
    useAgentStore.getState().setCurrentSession('session-1');
    useAgentStore.getState().addEvent({
      id: 1,
      sessionId: 'session-1',
      type: 'USER_MESSAGE',
      sequence: 1,
      data: {},
      timestamp: new Date(),
    });

    // Clear
    useAgentStore.getState().clearSession();

    expect(useAgentStore.getState().currentSessionId).toBeNull();
    expect(useAgentStore.getState().events).toEqual([]);
    expect(useAgentStore.getState().messages).toEqual([]);
  });
});
