import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MessageItem } from '../Chat/MessageItem';

describe('MessageItem', () => {
  it('renders user message', () => {
    const message = {
      id: 1,
      sessionId: 'test-session',
      role: 'USER' as const,
      content: 'Hello, agent!',
      createdAt: new Date(),
    };

    render(<MessageItem message={message} />);
    expect(screen.getByText('Hello, agent!')).toBeInTheDocument();
  });

  it('renders assistant message', () => {
    const message = {
      id: 2,
      sessionId: 'test-session',
      role: 'ASSISTANT' as const,
      content: 'How can I help you?',
      createdAt: new Date(),
    };

    render(<MessageItem message={message} />);
    expect(screen.getByText('How can I help you?')).toBeInTheDocument();
  });

  it('applies correct styling for user messages', () => {
    const message = {
      id: 1,
      sessionId: 'test-session',
      role: 'USER' as const,
      content: 'Test',
      createdAt: new Date(),
    };

    const { container } = render(<MessageItem message={message} />);
    expect(container.querySelector('.user-message')).toBeInTheDocument();
  });

  it('applies correct styling for assistant messages', () => {
    const message = {
      id: 2,
      sessionId: 'test-session',
      role: 'ASSISTANT' as const,
      content: 'Test',
      createdAt: new Date(),
    };

    const { container } = render(<MessageItem message={message} />);
    expect(container.querySelector('.assistant-message')).toBeInTheDocument();
  });
});
