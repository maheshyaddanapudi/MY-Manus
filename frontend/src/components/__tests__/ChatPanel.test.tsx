import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ChatPanel } from '../Chat/ChatPanel';

vi.mock('../../services/api');

describe('ChatPanel', () => {
  it('renders chat panel', () => {
    render(<ChatPanel sessionId="test-session" />);
    expect(screen.getByPlaceholderText(/Type a message/)).toBeInTheDocument();
  });

  it('submits message on send button click', async () => {
    const user = userEvent.setup();
    render(<ChatPanel sessionId="test-session" />);

    const input = screen.getByPlaceholderText(/Type a message/);
    await user.type(input, 'Hello, agent!');

    const sendButton = screen.getByText(/Send/);
    await user.click(sendButton);

    // Input should be cleared after sending
    expect(input).toHaveValue('');
  });

  it('submits message on Enter key', async () => {
    const user = userEvent.setup();
    render(<ChatPanel sessionId="test-session" />);

    const input = screen.getByPlaceholderText(/Type a message/);
    await user.type(input, 'Hello, agent!{Enter}');

    expect(input).toHaveValue('');
  });
});
