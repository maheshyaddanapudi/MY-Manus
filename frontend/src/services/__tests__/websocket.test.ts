import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { WebSocketService } from '../websocket';
import { Client } from '@stomp/stompjs';

// Mock @stomp/stompjs
let mockClientInstance: any;

vi.mock('@stomp/stompjs', () => {
  return {
    Client: class MockClient {
      activate = vi.fn();
      deactivate = vi.fn();
      subscribe = vi.fn();
      connected = false;
      onConnect: any = null;
      onStompError: any = null;
      onWebSocketClose: any = null;

      constructor(config: any) {
        mockClientInstance = this;
      }
    },
  };
});

// Mock sockjs-client
vi.mock('sockjs-client', () => {
  return {
    default: vi.fn().mockImplementation(() => ({})),
  };
});

describe('WebSocketService', () => {
  let service: WebSocketService;
  let mockClient: any;

  beforeEach(() => {
    vi.clearAllMocks();
    mockClientInstance = null;
    service = new WebSocketService();
  });

  afterEach(() => {
    service.disconnect();
  });

  it('creates WebSocket service instance', () => {
    expect(service).toBeDefined();
    expect(service.isConnected()).toBe(false);
  });

  it('connects to WebSocket with session ID', () => {
    const sessionId = 'test-session-123';
    const onEvent = vi.fn();

    service.connect(sessionId, onEvent);

    expect(mockClientInstance).toBeDefined();
    expect(mockClientInstance.activate).toHaveBeenCalled();
  });

  it('handles connection open and subscribes to topic', () => {
    const sessionId = 'test-session-123';
    const onEvent = vi.fn();
    const mockSubscription = { unsubscribe: vi.fn() };

    service.connect(sessionId, onEvent);
    mockClientInstance.subscribe = vi.fn().mockReturnValue(mockSubscription);

    // Simulate connection
    if (mockClientInstance.onConnect) {
      mockClientInstance.connected = true;
      mockClientInstance.onConnect();
    }

    expect(mockClientInstance.subscribe).toHaveBeenCalledWith(
      `/topic/agent/${sessionId}`,
      expect.any(Function)
    );
  });

  it('handles incoming events', () => {
    const sessionId = 'test-session-123';
    const onEvent = vi.fn();
    let messageHandler: any;

    service.connect(sessionId, onEvent);

    mockClientInstance.subscribe = vi.fn().mockImplementation((topic, handler) => {
      messageHandler = handler;
      return { unsubscribe: vi.fn() };
    });

    // Simulate connection
    if (mockClientInstance.onConnect) {
      mockClientInstance.connected = true;
      mockClientInstance.onConnect();
    }

    // Simulate incoming message
    const testEvent = {
      id: 'event-1',
      type: 'ITERATION_START',
      iteration: 1,
      content: 'Test event',
      timestamp: new Date().toISOString(),
    };

    messageHandler({ body: JSON.stringify(testEvent) });

    expect(onEvent).toHaveBeenCalledWith(testEvent);
  });

  it('disconnects and cleans up', () => {
    const sessionId = 'test-session-123';
    const onEvent = vi.fn();
    const mockSubscription = { unsubscribe: vi.fn() };

    service.connect(sessionId, onEvent);

    mockClientInstance.subscribe = vi.fn().mockReturnValue(mockSubscription);

    // Simulate connection
    if (mockClientInstance.onConnect) {
      mockClientInstance.connected = true;
      mockClientInstance.onConnect();
    }

    service.disconnect();

    expect(mockSubscription.unsubscribe).toHaveBeenCalled();
    expect(mockClientInstance.deactivate).toHaveBeenCalled();
  });

  it('handles connection errors', () => {
    const sessionId = 'test-session-123';
    const onEvent = vi.fn();
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    service.connect(sessionId, onEvent);

    // Simulate STOMP error
    if (mockClientInstance.onStompError) {
      mockClientInstance.onStompError({ command: 'ERROR', body: 'Test error' });
    }

    expect(consoleSpy).toHaveBeenCalledWith('STOMP error:', expect.any(Object));
    consoleSpy.mockRestore();
  });

  it('handles WebSocket close', () => {
    const sessionId = 'test-session-123';
    const onEvent = vi.fn();
    const onDisconnect = vi.fn();

    service.connect(sessionId, onEvent);
    service.onDisconnect(onDisconnect);

    // Simulate WebSocket close
    if (mockClientInstance.onWebSocketClose) {
      mockClientInstance.onWebSocketClose();
    }

    expect(onDisconnect).toHaveBeenCalled();
  });

  it('calls onConnect callback when connected', () => {
    const sessionId = 'test-session-123';
    const onEvent = vi.fn();
    const onConnect = vi.fn();

    service.connect(sessionId, onEvent);
    service.onConnect(onConnect);

    // Simulate connection
    if (mockClientInstance.onConnect) {
      mockClientInstance.connected = true;
      mockClientInstance.onConnect();
    }

    expect(onConnect).toHaveBeenCalled();
  });

  it('returns connection status', () => {
    expect(service.isConnected()).toBe(false);

    const sessionId = 'test-session-123';
    const onEvent = vi.fn();

    service.connect(sessionId, onEvent);

    // Simulate connection
    mockClientInstance.connected = true;

    expect(service.isConnected()).toBe(true);
  });
});
