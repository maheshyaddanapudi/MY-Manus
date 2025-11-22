import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MainLayout } from '../../components/Layout/MainLayout';
import * as apiService from '../../services/api';

vi.mock('../../services/api');
vi.mock('../../services/websocket');

describe('Session Switching Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('switches between multiple sessions', async () => {
    const user = userEvent.setup();

    const mockSessions = [
      {
        sessionId: 'session-1',
        title: 'Conversation 1',
        createdAt: new Date(),
        updatedAt: new Date(),
        active: true,
      },
      {
        sessionId: 'session-2',
        title: 'Conversation 2',
        createdAt: new Date(),
        updatedAt: new Date(),
        active: true,
      },
    ];

    (apiService.getSessions as any).mockResolvedValue(mockSessions);

    // Mock events for session-1
    const session1Events = [
      {
        id: 1,
        sessionId: 'session-1',
        type: 'USER_MESSAGE',
        data: { text: 'Message from session 1' },
        timestamp: new Date(),
      },
    ];

    // Mock events for session-2
    const session2Events = [
      {
        id: 2,
        sessionId: 'session-2',
        type: 'USER_MESSAGE',
        data: { text: 'Message from session 2' },
        timestamp: new Date(),
      },
    ];

    (apiService.getEventStream as any)
      .mockResolvedValueOnce(session1Events)
      .mockResolvedValueOnce(session2Events);

    (apiService.getMessages as any).mockResolvedValue([]);

    render(<MainLayout />);

    // Wait for sessions to load
    await waitFor(() => {
      expect(screen.getByText('Conversation 1')).toBeInTheDocument();
      expect(screen.getByText('Conversation 2')).toBeInTheDocument();
    });

    // Click on session 1
    await user.click(screen.getByText('Conversation 1'));

    await waitFor(() => {
      expect(apiService.getEventStream).toHaveBeenCalledWith('session-1');
    });

    // Click on session 2
    await user.click(screen.getByText('Conversation 2'));

    await waitFor(() => {
      expect(apiService.getEventStream).toHaveBeenCalledWith('session-2');
    });
  });

  it('loads session-specific data when switching', async () => {
    const user = userEvent.setup();

    const mockSessions = [
      { sessionId: 'session-1', title: 'Session 1', createdAt: new Date(), updatedAt: new Date(), active: true },
    ];

    (apiService.getSessions as any).mockResolvedValue(mockSessions);
    (apiService.getEventStream as any).mockResolvedValue([]);
    (apiService.getMessages as any).mockResolvedValue([]);
    (apiService.getToolExecutions as any).mockResolvedValue([]);

    render(<MainLayout />);

    await waitFor(() => {
      expect(screen.getByText('Session 1')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Session 1'));

    await waitFor(() => {
      expect(apiService.getEventStream).toHaveBeenCalledWith('session-1');
      expect(apiService.getMessages).toHaveBeenCalledWith('session-1');
      expect(apiService.getToolExecutions).toHaveBeenCalledWith('session-1');
    });
  });
});
