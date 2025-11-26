import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ChatInput } from '../Chat/ChatInput';

describe('ChatInput', () => {
  it('renders chat input', () => {
    render(<ChatInput onSend={vi.fn()} />);
    expect(screen.getByPlaceholderText(/Type a message/)).toBeInTheDocument();
  });

  it('calls onSend when clicking send button', async () => {
    const onSend = vi.fn();
    const user = userEvent.setup();

    render(<ChatInput onSend={onSend} />);

    const input = screen.getByPlaceholderText(/Type a message/);
    await user.type(input, 'Test message');

    const sendButton = screen.getByText(/Send/);
    await user.click(sendButton);

    expect(onSend).toHaveBeenCalledWith('Test message');
  });

  it('clears input after sending', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSend={vi.fn()} />);

    const input = screen.getByPlaceholderText(/Type a message/);
    await user.type(input, 'Test message');
    await user.click(screen.getByText(/Send/));

    expect(input).toHaveValue('');
  });

  it('submits on Enter key', async () => {
    const onSend = vi.fn();
    const user = userEvent.setup();

    render(<ChatInput onSend={onSend} />);

    const input = screen.getByPlaceholderText(/Type a message/);
    await user.type(input, 'Test message{Enter}');

    expect(onSend).toHaveBeenCalledWith('Test message');
  });

  it('does not submit empty messages', async () => {
    const onSend = vi.fn();
    const user = userEvent.setup();

    render(<ChatInput onSend={onSend} />);

    const sendButton = screen.getByText(/Send/);
    await user.click(sendButton);

    expect(onSend).not.toHaveBeenCalled();
  });
});
