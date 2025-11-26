import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { EventStreamPanel } from '../../components/EventStream/EventStreamPanel';
import * as apiService from '../../services/api';

vi.mock('../../services/api');

describe('Event Stream Flow Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('displays complete event stream', async () => {
    const mockEvents = [
      {
        id: 1,
        sessionId: 'session-1',
        type: 'USER_MESSAGE' as const,
        sequence: 1,
        data: { text: 'Read file' },
        timestamp: new Date(),
      },
      {
        id: 2,
        sessionId: 'session-1',
        type: 'AGENT_THOUGHT' as const,
        sequence: 2,
        data: { thought: 'I will read the file' },
        timestamp: new Date(),
      },
      {
        id: 3,
        sessionId: 'session-1',
        type: 'AGENT_ACTION' as const,
        sequence: 3,
        data: { pythonCode: 'file_read("/workspace/test.txt")' },
        timestamp: new Date(),
      },
      {
        id: 4,
        sessionId: 'session-1',
        type: 'OBSERVATION' as const,
        sequence: 4,
        data: { stdout: 'File content', exitCode: 0 },
        timestamp: new Date(),
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<EventStreamPanel sessionId="session-1" />);

    await waitFor(() => {
      expect(screen.getByText(/Read file/)).toBeInTheDocument();
      expect(screen.getByText(/I will read the file/)).toBeInTheDocument();
      expect(screen.getByText(/file_read/)).toBeInTheDocument();
      expect(screen.getByText(/File content/)).toBeInTheDocument();
    });
  });

  it('updates stream in real-time', async () => {
    const initialEvents = [
      {
        id: 1,
        sessionId: 'session-1',
        type: 'USER_MESSAGE' as const,
        sequence: 1,
        data: { text: 'Hello' },
        timestamp: new Date(),
      },
    ];

    (apiService.getEventStream as any).mockResolvedValueOnce(initialEvents);

    const { rerender } = render(<EventStreamPanel sessionId="session-1" autoRefresh={true} refreshInterval={1000} />);

    await waitFor(() => {
      expect(screen.getByText(/Hello/)).toBeInTheDocument();
    });

    // Add new event
    const updatedEvents = [
      ...initialEvents,
      {
        id: 2,
        sessionId: 'session-1',
        type: 'AGENT_THOUGHT' as const,
        sequence: 2,
        data: { thought: 'Processing' },
        timestamp: new Date(),
      },
    ];

    (apiService.getEventStream as any).mockResolvedValueOnce(updatedEvents);

    // Trigger refresh
    rerender(<EventStreamPanel sessionId="session-1" autoRefresh={true} refreshInterval={1000} />);

    await waitFor(() => {
      expect(screen.getByText(/Processing/)).toBeInTheDocument();
    });
  });
});
