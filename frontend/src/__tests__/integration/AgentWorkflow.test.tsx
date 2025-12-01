import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MainLayout } from '../../components/Layout/MainLayout';
import * as apiService from '../../services/api';

vi.mock('../../services/api');
vi.mock('../../services/websocket');

describe('Agent Workflow Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    // Mock initial empty state
    (apiService.getSessions as any).mockResolvedValue([]);
    (apiService.getEventStream as any).mockResolvedValue([]);
    (apiService.getMessages as any).mockResolvedValue([]);
  });

  it('completes full agent workflow', async () => {
    const user = userEvent.setup();

    // Mock create session
    (apiService.createSession as any).mockResolvedValue({
      sessionId: 'test-session',
      title: 'New Conversation',
    });

    // Mock send message
    (apiService.sendMessage as any).mockResolvedValue({ success: true });

    // Mock event stream updates
    const mockEvents = [
      {
        id: 1,
        type: 'USER_MESSAGE',
        sequence: 1,
        data: { text: 'Hello' },
        timestamp: new Date(),
      },
      {
        id: 2,
        type: 'AGENT_THOUGHT',
        sequence: 2,
        data: { thought: 'Processing request' },
        timestamp: new Date(),
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<MainLayout />);

    // Create new session
    const newButton = screen.getByText(/New Conversation/);
    await user.click(newButton);

    await waitFor(() => {
      expect(apiService.createSession).toHaveBeenCalled();
    });

    // Send message
    const input = screen.getByPlaceholderText(/Type a message/);
    await user.type(input, 'Hello{Enter}');

    await waitFor(() => {
      expect(apiService.sendMessage).toHaveBeenCalledWith('test-session', 'Hello');
    });

    // Verify events displayed
    await waitFor(() => {
      expect(screen.getByText(/Processing request/)).toBeInTheDocument();
    });
  });
});
