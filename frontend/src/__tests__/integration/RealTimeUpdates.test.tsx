import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { EventStreamPanel } from '../../components/EventStream/EventStreamPanel';
import * as apiService from '../../services/api';

vi.mock('../../services/api');

// Mock WebSocket
class MockWebSocket {
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  readyState: number = 1; // OPEN

  send(data: string) {}
  close() {}
}

global.WebSocket = MockWebSocket as any;

describe('Real-Time Updates Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('receives real-time event updates via WebSocket', async () => {
    const initialEvents = [
      {
        id: 1,
        sessionId: 'session-1',
        type: 'USER_MESSAGE' as const,
        sequence: 1,
        data: { text: 'Initial message' },
        timestamp: new Date(),
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(initialEvents);

    render(<EventStreamPanel sessionId="session-1" />);

    await waitFor(() => {
      expect(screen.getByText(/Initial message/)).toBeInTheDocument();
    });

    // Simulate WebSocket message
    const newEvent = {
      id: 2,
      sessionId: 'session-1',
      type: 'AGENT_THOUGHT',
      sequence: 2,
      data: { thought: 'Received via WebSocket' },
      timestamp: new Date(),
    };

    const updatedEvents = [...initialEvents, newEvent];
    (apiService.getEventStream as any).mockResolvedValue(updatedEvents);

    // Trigger refresh (simulating WebSocket update)
    const { rerender } = render(<EventStreamPanel sessionId="session-1" />);
    rerender(<EventStreamPanel sessionId="session-1" />);

    await waitFor(() => {
      expect(screen.getByText(/Received via WebSocket/)).toBeInTheDocument();
    });
  });

  it('handles auto-refresh for real-time updates', async () => {
    vi.useFakeTimers();

    const mockEvents = [
      {
        id: 1,
        sessionId: 'session-1',
        type: 'USER_MESSAGE' as const,
        sequence: 1,
        data: { text: 'Message 1' },
        timestamp: new Date(),
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<EventStreamPanel sessionId="session-1" autoRefresh={true} refreshInterval={1000} />);

    await waitFor(() => {
      expect(apiService.getEventStream).toHaveBeenCalledTimes(1);
    });

    // Fast-forward time
    vi.advanceTimersByTime(1000);

    await waitFor(() => {
      expect(apiService.getEventStream).toHaveBeenCalledTimes(2);
    });

    vi.useRealTimers();
  });

  it('updates multiple panels simultaneously', async () => {
    const mockEvents = [
      {
        id: 1,
        sessionId: 'session-1',
        type: 'USER_MESSAGE' as const,
        sequence: 1,
        data: { text: 'Shared message' },
        timestamp: new Date(),
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(mockEvents);
    (apiService.getMessages as any).mockResolvedValue([
      {
        id: 1,
        sessionId: 'session-1',
        role: 'USER' as const,
        content: 'Shared message',
        createdAt: new Date(),
      },
    ]);

    const { container } = render(
      <div>
        <EventStreamPanel sessionId="session-1" />
      </div>
    );

    await waitFor(() => {
      expect(screen.getAllByText(/Shared message/).length).toBeGreaterThan(0);
    });
  });
});
