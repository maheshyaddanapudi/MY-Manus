import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useWebSocket } from '../useWebSocket';

// Mock WebSocket
class MockWebSocket {
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;

  send(data: string) {
    // Mock send
  }

  close() {
    if (this.onclose) {
      this.onclose(new CloseEvent('close'));
    }
  }
}

global.WebSocket = MockWebSocket as any;

describe('useWebSocket', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('initializes WebSocket connection', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8080/ws'));
    expect(result.current.isConnected).toBe(false);
  });

  it('handles connection open', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8080/ws'));

    // Simulate WebSocket open
    const ws = result.current.socket;
    if (ws && ws.onopen) {
      ws.onopen(new Event('open'));
    }

    expect(result.current.isConnected).toBe(true);
  });

  it('handles incoming messages', () => {
    const onMessage = vi.fn();
    const { result } = renderHook(() => useWebSocket('ws://localhost:8080/ws', { onMessage }));

    // Simulate message
    const ws = result.current.socket;
    if (ws && ws.onmessage) {
      const message = new MessageEvent('message', { data: JSON.stringify({ type: 'event' }) });
      ws.onmessage(message);
    }

    expect(onMessage).toHaveBeenCalled();
  });

  it('handles connection close', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8080/ws'));

    // Open connection first
    const ws = result.current.socket;
    if (ws && ws.onopen) {
      ws.onopen(new Event('open'));
    }

    // Close connection
    if (ws && ws.onclose) {
      ws.onclose(new CloseEvent('close'));
    }

    expect(result.current.isConnected).toBe(false);
  });

  it('cleans up on unmount', () => {
    const { result, unmount } = renderHook(() => useWebSocket('ws://localhost:8080/ws'));

    const closeSpy = vi.spyOn(result.current.socket!, 'close');

    unmount();

    expect(closeSpy).toHaveBeenCalled();
  });
});
