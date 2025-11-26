import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { EventStreamPanel } from '../EventStream/EventStreamPanel';
import { apiService } from '../../services/api';

// Mock API service
vi.mock('../../services/api', () => ({
  apiService: {
    getEventStream: vi.fn(),
    getEventsForIteration: vi.fn(),
  },
}));

/**
 * Test Suite for EventStreamPanel Component
 * Coverage Target: 100%
 *
 * Tests:
 * - Component rendering
 * - Event fetching
 * - Auto-refresh functionality
 * - Iteration filtering
 * - Event display
 * - Error handling
 * - Loading states
 */
describe('EventStreamPanel', () => {
  const mockSessionId = 'test-session-123';

  const mockEvents = [
    {
      id: '1',
      type: 'USER_MESSAGE',
      iteration: 1,
      sequence: 0,
      content: 'Test user message',
      timestamp: new Date().toISOString(),
    },
    {
      id: '2',
      type: 'AGENT_THOUGHT',
      iteration: 1,
      sequence: 1,
      content: 'Agent thinking...',
      timestamp: new Date().toISOString(),
    },
    {
      id: '3',
      type: 'AGENT_ACTION',
      iteration: 1,
      sequence: 2,
      content: 'file_read("test.txt")',
      timestamp: new Date().toISOString(),
      data: { tool: 'file_read' },
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders without crashing', () => {
    (apiService.getEventStream as any).mockResolvedValue([]);

    render(<EventStreamPanel sessionId={mockSessionId} />);

    expect(screen.getByText('Event Stream')).toBeInTheDocument();
  });

  it('fetches and displays events', async () => {
    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<EventStreamPanel sessionId={mockSessionId} />);

    await waitFor(() => {
      expect(apiService.getEventStream).toHaveBeenCalledWith(mockSessionId);
    });

    await waitFor(() => {
      expect(screen.getByText(/USER_MESSAGE/i)).toBeInTheDocument();
      expect(screen.getByText(/AGENT_THOUGHT/i)).toBeInTheDocument();
    });
  });

  it('displays event count', async () => {
    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<EventStreamPanel sessionId={mockSessionId} />);

    await waitFor(() => {
      expect(screen.getByText('3 events')).toBeInTheDocument();
    });
  });

  it('handles empty event stream', async () => {
    (apiService.getEventStream as any).mockResolvedValue([]);

    render(<EventStreamPanel sessionId={mockSessionId} />);

    await waitFor(() => {
      expect(screen.getByText('No events yet')).toBeInTheDocument();
    });
  });

  it('handles API errors gracefully', async () => {
    (apiService.getEventStream as any).mockRejectedValue(new Error('API Error'));

    render(<EventStreamPanel sessionId={mockSessionId} />);

    await waitFor(() => {
      expect(screen.getByText(/API Error/i)).toBeInTheDocument();
    });
  });

  it('supports auto-refresh', async () => {
    vi.useFakeTimers();
    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<EventStreamPanel sessionId={mockSessionId} autoRefresh={true} refreshInterval={1000} />);

    await waitFor(() => {
      expect(apiService.getEventStream).toHaveBeenCalledTimes(1);
    });

    // Fast-forward 1 second
    vi.advanceTimersByTime(1000);

    await waitFor(() => {
      expect(apiService.getEventStream).toHaveBeenCalledTimes(2);
    });

    vi.useRealTimers();
  });

  it('filters events by iteration', async () => {
    (apiService.getEventStream as any).mockResolvedValue(mockEvents);
    (apiService.getEventsForIteration as any).mockResolvedValue([mockEvents[0]]);

    const { rerender } = render(<EventStreamPanel sessionId={mockSessionId} />);

    // TODO: Test iteration filter interaction
    // This requires user interaction testing with fireEvent

    expect(apiService.getEventStream).toHaveBeenCalled();
  });
});
