import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MessageList } from '../Chat/MessageList';
import * as apiService from '../../services/api';

vi.mock('../../services/api');

describe('MessageList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders message list', () => {
    render(<MessageList sessionId="test-session" />);
    expect(screen.getByTestId('message-list')).toBeInTheDocument();
  });

  it('fetches and displays messages', async () => {
    const mockMessages = [
      {
        id: 1,
        sessionId: 'test-session',
        role: 'USER',
        content: 'Hello',
        createdAt: new Date(),
      },
      {
        id: 2,
        sessionId: 'test-session',
        role: 'ASSISTANT',
        content: 'Hi there!',
        createdAt: new Date(),
      },
    ];

    (apiService.getMessages as any).mockResolvedValue(mockMessages);

    render(<MessageList sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText('Hello')).toBeInTheDocument();
      expect(screen.getByText('Hi there!')).toBeInTheDocument();
    });
  });

  it('displays empty state when no messages', async () => {
    (apiService.getMessages as any).mockResolvedValue([]);

    render(<MessageList sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText(/No messages yet/)).toBeInTheDocument();
    });
  });
});
