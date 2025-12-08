import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ChatInput } from '../Chat/ChatInput';

describe('ChatInput', () => {
  it('renders chat input', () => {
    render(<ChatInput onSend={vi.fn()} />);
    expect(screen.getByPlaceholderText(/Ask the agent to solve a task/)).toBeInTheDocument();
  });

  it('calls onSend when clicking send button', async () => {
    const onSend = vi.fn();
    const user = userEvent.setup();

    render(<ChatInput onSend={onSend} />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
    await user.type(input, 'Test message');

    const sendButton = screen.getByText(/Send/);
    await user.click(sendButton);

    expect(onSend).toHaveBeenCalledWith('Test message');
  });

  it('clears input after sending', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSend={vi.fn()} />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
    await user.type(input, 'Test message');
    await user.click(screen.getByText(/Send/));

    expect(input).toHaveValue('');
  });

  it('submits on Enter key', async () => {
    const onSend = vi.fn();
    const user = userEvent.setup();

    render(<ChatInput onSend={onSend} />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
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

  it('shows Stop button when agent is running and input is empty', () => {
    render(<ChatInput onSend={vi.fn()} onStop={vi.fn()} isAgentRunning={true} />);

    expect(screen.getByText(/Stop/)).toBeInTheDocument();
    expect(screen.queryByText(/Send/)).not.toBeInTheDocument();
  });

  it('shows Send button when agent is running but input has text', async () => {
    const user = userEvent.setup();
    render(<ChatInput onSend={vi.fn()} onStop={vi.fn()} isAgentRunning={true} />);

    const input = screen.getByPlaceholderText(/Type a follow-up/);
    await user.type(input, 'Follow-up message');

    expect(screen.getByText(/Send/)).toBeInTheDocument();
    expect(screen.queryByText(/Stop/)).not.toBeInTheDocument();
  });

  it('calls onStop when clicking Stop button', async () => {
    const onStop = vi.fn();
    const user = userEvent.setup();

    render(<ChatInput onSend={vi.fn()} onStop={onStop} isAgentRunning={true} />);

    const stopButton = screen.getByText(/Stop/);
    await user.click(stopButton);

    expect(onStop).toHaveBeenCalled();
  });

  it('shows different placeholder when agent is running', () => {
    render(<ChatInput onSend={vi.fn()} isAgentRunning={true} />);
    expect(screen.getByPlaceholderText(/Type a follow-up/)).toBeInTheDocument();
  });

  it('shows different hint text when agent is running', () => {
    render(<ChatInput onSend={vi.fn()} isAgentRunning={true} />);
    expect(screen.getByText(/Agent is working/)).toBeInTheDocument();
  });

  it('textarea is not disabled when agent is running', () => {
    render(<ChatInput onSend={vi.fn()} isAgentRunning={true} />);
    const textarea = screen.getByPlaceholderText(/Type a follow-up/);
    expect(textarea).not.toBeDisabled();
  });
});
