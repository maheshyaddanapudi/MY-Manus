import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MessageList } from '../Chat/MessageList';
import type { Message } from '../../types';

// Mock MessageItem to simplify testing
vi.mock('../Chat/MessageItem', () => ({
  MessageItem: ({ message }: { message: Message }) => (
    <div data-testid="message-item">
      <div>{message.role === 'user' ? '👤 You' : '🤖 Assistant'}</div>
      <div>{message.content}</div>
    </div>
  ),
}));

describe('MessageList', () => {
  beforeEach(() => {
    // Mock scrollIntoView which is not available in jsdom
    Element.prototype.scrollIntoView = vi.fn();
  });
  it('renders empty state when no messages', () => {
    render(<MessageList messages={[]} />);
    
    expect(screen.getByText('Welcome to MY Manus')).toBeInTheDocument();
    expect(screen.getByText(/I'm an AI agent/)).toBeInTheDocument();
  });

  it('renders messages when provided', () => {
    const messages: Message[] = [
      {
        id: '1',
        role: 'user',
        content: 'Hello, agent!',
        timestamp: new Date('2024-01-01T10:00:00Z'),
      },
      {
        id: '2',
        role: 'assistant',
        content: 'Hi there! How can I help you?',
        timestamp: new Date('2024-01-01T10:00:01Z'),
      },
    ];

    render(<MessageList messages={messages} />);
    
    expect(screen.getByText('Hello, agent!')).toBeInTheDocument();
    expect(screen.getByText('Hi there! How can I help you?')).toBeInTheDocument();
  });

  it('renders multiple messages', () => {
    const messages: Message[] = [
      {
        id: '1',
        role: 'user',
        content: 'First message',
        timestamp: new Date('2024-01-01T10:00:00Z'),
      },
      {
        id: '2',
        role: 'assistant',
        content: 'Second message',
        timestamp: new Date('2024-01-01T10:00:01Z'),
      },
      {
        id: '3',
        role: 'user',
        content: 'Third message',
        timestamp: new Date('2024-01-01T10:00:02Z'),
      },
    ];

    render(<MessageList messages={messages} />);
    
    const messageItems = screen.getAllByTestId('message-item');
    expect(messageItems).toHaveLength(3);
    
    expect(screen.getByText('First message')).toBeInTheDocument();
    expect(screen.getByText('Second message')).toBeInTheDocument();
    expect(screen.getByText('Third message')).toBeInTheDocument();
  });

  it('renders user and assistant messages correctly', () => {
    const messages: Message[] = [
      {
        id: '1',
        role: 'user',
        content: 'User message',
        timestamp: new Date('2024-01-01T10:00:00Z'),
      },
      {
        id: '2',
        role: 'assistant',
        content: 'Assistant message',
        timestamp: new Date('2024-01-01T10:00:01Z'),
      },
    ];

    render(<MessageList messages={messages} />);
    
    expect(screen.getByText('👤 You')).toBeInTheDocument();
    expect(screen.getByText('🤖 Assistant')).toBeInTheDocument();
  });

  it('renders system messages', () => {
    const messages: Message[] = [
      {
        id: '1',
        role: 'system',
        content: 'System notification',
        timestamp: new Date('2024-01-01T10:00:00Z'),
      },
    ];

    render(<MessageList messages={messages} />);
    
    expect(screen.getByText('System notification')).toBeInTheDocument();
  });

  it('does not show empty state when messages exist', () => {
    const messages: Message[] = [
      {
        id: '1',
        role: 'user',
        content: 'Hello',
        timestamp: new Date('2024-01-01T10:00:00Z'),
      },
    ];

    render(<MessageList messages={messages} />);
    
    expect(screen.queryByText('Welcome to MY Manus')).not.toBeInTheDocument();
  });

  it('renders messages in order', () => {
    const messages: Message[] = [
      {
        id: '1',
        role: 'user',
        content: 'Message 1',
        timestamp: new Date('2024-01-01T10:00:00Z'),
      },
      {
        id: '2',
        role: 'assistant',
        content: 'Message 2',
        timestamp: new Date('2024-01-01T10:00:01Z'),
      },
      {
        id: '3',
        role: 'user',
        content: 'Message 3',
        timestamp: new Date('2024-01-01T10:00:02Z'),
      },
    ];

    render(<MessageList messages={messages} />);
    
    const messageItems = screen.getAllByTestId('message-item');
    
    // Check order by checking text content
    expect(messageItems[0]).toHaveTextContent('Message 1');
    expect(messageItems[1]).toHaveTextContent('Message 2');
    expect(messageItems[2]).toHaveTextContent('Message 3');
  });
});
