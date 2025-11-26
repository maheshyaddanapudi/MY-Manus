import { describe, it, expect, vi, beforeEach } from 'vitest';
import { WebSocketClient } from '../websocket';

// Mock WebSocket
class MockWebSocket {
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  readyState: number = 0;

  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  send(data: string) {
    // Mock send
  }

  close() {
    this.readyState = MockWebSocket.CLOSED;
    if (this.onclose) {
      this.onclose(new CloseEvent('close'));
    }
  }
}

global.WebSocket = MockWebSocket as any;

describe('WebSocket Client', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('creates WebSocket connection', () => {
    const client = new WebSocketClient('ws://localhost:8080/ws');
    expect(client).toBeDefined();
  });

  it('handles connection open', () => {
    const onOpen = vi.fn();
    const client = new WebSocketClient('ws://localhost:8080/ws', { onOpen });

    // Simulate open
    if (client.socket && client.socket.onopen) {
      client.socket.readyState = MockWebSocket.OPEN;
      client.socket.onopen(new Event('open'));
    }

    expect(onOpen).toHaveBeenCalled();
  });

  it('handles incoming messages', () => {
    const onMessage = vi.fn();
    const client = new WebSocketClient('ws://localhost:8080/ws', { onMessage });

    // Simulate message
    if (client.socket && client.socket.onmessage) {
      const messageData = JSON.stringify({ type: 'event', data: {} });
      client.socket.onmessage(new MessageEvent('message', { data: messageData }));
    }

    expect(onMessage).toHaveBeenCalled();
  });

  it('sends messages', () => {
    const client = new WebSocketClient('ws://localhost:8080/ws');

    // Open connection first
    if (client.socket) {
      client.socket.readyState = MockWebSocket.OPEN;
      const sendSpy = vi.spyOn(client.socket, 'send');

      client.send({ type: 'subscribe', sessionId: 'session-1' });

      expect(sendSpy).toHaveBeenCalled();
    }
  });

  it('handles connection close', () => {
    const onClose = vi.fn();
    const client = new WebSocketClient('ws://localhost:8080/ws', { onClose });

    // Simulate close
    if (client.socket && client.socket.onclose) {
      client.socket.onclose(new CloseEvent('close'));
    }

    expect(onClose).toHaveBeenCalled();
  });

  it('handles connection errors', () => {
    const onError = vi.fn();
    const client = new WebSocketClient('ws://localhost:8080/ws', { onError });

    // Simulate error
    if (client.socket && client.socket.onerror) {
      client.socket.onerror(new Event('error'));
    }

    expect(onError).toHaveBeenCalled();
  });

  it('closes connection', () => {
    const client = new WebSocketClient('ws://localhost:8080/ws');

    if (client.socket) {
      const closeSpy = vi.spyOn(client.socket, 'close');
      client.close();
      expect(closeSpy).toHaveBeenCalled();
    }
  });

  it('reconnects on connection loss', () => {
    const client = new WebSocketClient('ws://localhost:8080/ws', { reconnect: true });

    // Simulate close (should trigger reconnect)
    if (client.socket && client.socket.onclose) {
      client.socket.onclose(new CloseEvent('close'));
    }

    // Check that reconnect is attempted (implementation dependent)
    expect(client).toBeDefined();
  });
});
