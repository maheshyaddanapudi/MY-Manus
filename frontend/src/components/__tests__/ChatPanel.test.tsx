import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ChatPanel } from '../Chat/ChatPanel';
import { useAgentStore } from '../../stores/agentStore';
import { apiService } from '../../services/api';

// Mock agentStore
vi.mock('../../stores/agentStore');

// Mock apiService
vi.mock('../../services/api', () => ({
  apiService: {
    chat: vi.fn(),
  },
}));

// Mock MessageList to simplify testing
vi.mock('../Chat/MessageList', () => ({
  MessageList: ({ messages }: any) => (
    <div data-testid="message-list">
      {messages.map((msg: any) => (
        <div key={msg.id}>{msg.content}</div>
      ))}
    </div>
  ),
}));

// Mock scrollIntoView for MessageList
beforeAll(() => {
  Element.prototype.scrollIntoView = vi.fn();
});

describe('ChatPanel', () => {
  const mockAddMessage = vi.fn();
  const mockSetAgentStatus = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    
    // Default mock for useAgentStore
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123',
      messages: [],
      addMessage: mockAddMessage,
      setAgentStatus: mockSetAgentStatus,
    });

    // Default mock for apiService.chat
    (apiService.chat as any).mockResolvedValue({
      sessionId: 'test-session-123',
      message: 'Response from agent',
    });
  });

  it('renders chat panel with message list and input', () => {
    render(<ChatPanel />);
    
    expect(screen.getByTestId('message-list')).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Ask the agent to solve a task/)).toBeInTheDocument();
    expect(screen.getByText('Send')).toBeInTheDocument();
  });

  it('displays existing messages', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123',
      messages: [
        { id: '1', role: 'user', content: 'Hello', timestamp: new Date() },
        { id: '2', role: 'assistant', content: 'Hi there!', timestamp: new Date() },
      ],
      addMessage: mockAddMessage,
      setAgentStatus: mockSetAgentStatus,
    });

    render(<ChatPanel />);
    
    expect(screen.getByText('Hello')).toBeInTheDocument();
    expect(screen.getByText('Hi there!')).toBeInTheDocument();
  });

  it('submits message on send button click', async () => {
    const user = userEvent.setup();
    render(<ChatPanel />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
    await user.type(input, 'Hello, agent!');

    const sendButton = screen.getByText('Send');
    await user.click(sendButton);

    // Should add user message to store
    await waitFor(() => {
      expect(mockAddMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'user',
          content: 'Hello, agent!',
        })
      );
    });

    // Should call API
    await waitFor(() => {
      expect(apiService.chat).toHaveBeenCalledWith({
        sessionId: 'test-session-123',
        message: 'Hello, agent!',
      });
    });

    // Should set agent status to thinking
    expect(mockSetAgentStatus).toHaveBeenCalledWith('thinking');

    // Input should be cleared after sending
    expect(input).toHaveValue('');
  });

  it('submits message on Enter key', async () => {
    const user = userEvent.setup();
    render(<ChatPanel />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
    await user.type(input, 'Hello, agent!{Enter}');

    // Should add user message
    await waitFor(() => {
      expect(mockAddMessage).toHaveBeenCalled();
    });

    // Should call API
    await waitFor(() => {
      expect(apiService.chat).toHaveBeenCalled();
    });

    // Input should be cleared
    expect(input).toHaveValue('');
  });

  it('does not submit empty messages', async () => {
    const user = userEvent.setup();
    render(<ChatPanel />);

    const sendButton = screen.getByText('Send');
    await user.click(sendButton);

    // Should not add message or call API
    expect(mockAddMessage).not.toHaveBeenCalled();
    expect(apiService.chat).not.toHaveBeenCalled();
  });

  it('does not submit whitespace-only messages', async () => {
    const user = userEvent.setup();
    render(<ChatPanel />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
    await user.type(input, '   ');

    const sendButton = screen.getByText('Send');
    await user.click(sendButton);

    // Should not add message or call API
    expect(mockAddMessage).not.toHaveBeenCalled();
    expect(apiService.chat).not.toHaveBeenCalled();
  });

  it('handles API errors gracefully', async () => {
    (apiService.chat as any).mockRejectedValue(new Error('Network error'));

    const user = userEvent.setup();
    render(<ChatPanel />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
    await user.type(input, 'Hello');

    const sendButton = screen.getByText('Send');
    await user.click(sendButton);

    // Should still add user message
    await waitFor(() => {
      expect(mockAddMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'user',
          content: 'Hello',
        })
      );
    });

    // Should set error status
    await waitFor(() => {
      expect(mockSetAgentStatus).toHaveBeenCalledWith('error');
    });

    // Should add error message
    await waitFor(() => {
      expect(mockAddMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          role: 'system',
          content: expect.stringContaining('Error'),
        })
      );
    });
  });

  it('prevents multiple simultaneous submissions', async () => {
    // Make API call slow
    (apiService.chat as any).mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 1000))
    );

    const user = userEvent.setup();
    render(<ChatPanel />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
    await user.type(input, 'First message');

    const sendButton = screen.getByText('Send');
    
    // Click send button
    await user.click(sendButton);
    
    // Try to type and send again immediately
    await user.type(input, 'Second message');
    await user.click(sendButton);

    // Should only call API once (second call blocked by isProcessing)
    await waitFor(() => {
      expect(apiService.chat).toHaveBeenCalledTimes(1);
    });
  });

  it('uses sessionId from store when calling API', async () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: 'custom-session-456',
      messages: [],
      addMessage: mockAddMessage,
      setAgentStatus: mockSetAgentStatus,
    });

    const user = userEvent.setup();
    render(<ChatPanel />);

    const input = screen.getByPlaceholderText(/Ask the agent to solve a task/);
    await user.type(input, 'Test{Enter}');

    await waitFor(() => {
      expect(apiService.chat).toHaveBeenCalledWith({
        sessionId: 'custom-session-456',
        message: 'Test',
      });
    });
  });
});
