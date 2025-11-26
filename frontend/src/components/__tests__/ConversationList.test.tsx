import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConversationList } from '../Layout/ConversationList';
import * as apiService from '../../services/api';

vi.mock('../../services/api');

describe('ConversationList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders conversation list', () => {
    render(<ConversationList onSelectSession={vi.fn()} />);
    expect(screen.getByText(/Conversations/)).toBeInTheDocument();
  });

  it('fetches and displays conversations', async () => {
    const mockSessions = [
      {
        sessionId: 'session-1',
        title: 'Conversation 1',
        createdAt: new Date(),
      },
      {
        sessionId: 'session-2',
        title: 'Conversation 2',
        createdAt: new Date(),
      },
    ];

    (apiService.getSessions as any).mockResolvedValue(mockSessions);

    render(<ConversationList onSelectSession={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText('Conversation 1')).toBeInTheDocument();
      expect(screen.getByText('Conversation 2')).toBeInTheDocument();
    });
  });

  it('calls onSelectSession when clicking a conversation', async () => {
    const onSelectSession = vi.fn();
    const user = userEvent.setup();

    const mockSessions = [
      {
        sessionId: 'session-1',
        title: 'Test Conversation',
        createdAt: new Date(),
      },
    ];

    (apiService.getSessions as any).mockResolvedValue(mockSessions);

    render(<ConversationList onSelectSession={onSelectSession} />);

    await waitFor(() => {
      expect(screen.getByText('Test Conversation')).toBeInTheDocument();
    });

    await user.click(screen.getByText('Test Conversation'));
    expect(onSelectSession).toHaveBeenCalledWith('session-1');
  });

  it('displays empty state when no conversations', async () => {
    (apiService.getSessions as any).mockResolvedValue([]);

    render(<ConversationList onSelectSession={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByText(/No conversations yet/)).toBeInTheDocument();
    });
  });
});
