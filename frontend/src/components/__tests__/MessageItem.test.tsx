import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MessageItem } from '../Chat/MessageItem';
import type { Message } from '../../types';

describe('MessageItem', () => {
  it('renders user message', () => {
    const message: Message = {
      id: '1',
      role: 'user',
      content: 'Hello, agent!',
      timestamp: new Date('2024-01-01T10:00:00Z'),
    };

    render(<MessageItem message={message} />);
    expect(screen.getByText('Hello, agent!')).toBeInTheDocument();
    expect(screen.getByText('You')).toBeInTheDocument();
  });

  it('renders assistant message', () => {
    const message: Message = {
      id: '2',
      role: 'assistant',
      content: 'How can I help you?',
      timestamp: new Date('2024-01-01T10:00:01Z'),
    };

    render(<MessageItem message={message} />);
    expect(screen.getByText('How can I help you?')).toBeInTheDocument();
    expect(screen.getByText('MY Manus')).toBeInTheDocument();
  });

  it('renders system message', () => {
    const message: Message = {
      id: '3',
      role: 'system',
      content: 'System notification',
      timestamp: new Date('2024-01-01T10:00:02Z'),
    };

    render(<MessageItem message={message} />);
    expect(screen.getByText('System notification')).toBeInTheDocument();
    expect(screen.getByText('System')).toBeInTheDocument();
  });

  it('applies correct styling for user messages', () => {
    const message: Message = {
      id: '1',
      role: 'user',
      content: 'Test',
      timestamp: new Date('2024-01-01T10:00:00Z'),
    };

    const { container } = render(<MessageItem message={message} />);
    
    // User messages should have blue gradient background
    const messageElement = container.querySelector('.from-blue-600');
    expect(messageElement).toBeInTheDocument();
    expect(messageElement).toHaveClass('text-white');
  });

  it('applies correct styling for assistant messages', () => {
    const message: Message = {
      id: '2',
      role: 'assistant',
      content: 'Test',
      timestamp: new Date('2024-01-01T10:00:01Z'),
    };

    const { container } = render(<MessageItem message={message} />);
    
    // Assistant messages should have gray gradient background
    const messageElement = container.querySelector('.from-gray-800');
    expect(messageElement).toBeInTheDocument();
    expect(messageElement).toHaveClass('text-gray-100');
  });

  it('applies correct styling for system messages', () => {
    const message: Message = {
      id: '3',
      role: 'system',
      content: 'Test',
      timestamp: new Date('2024-01-01T10:00:02Z'),
    };

    const { container } = render(<MessageItem message={message} />);
    
    // System messages should have yellow gradient background with opacity
    const messageElement = container.querySelector('.from-yellow-900\\/40');
    expect(messageElement).toBeInTheDocument();
    expect(messageElement).toHaveClass('text-yellow-100');
  });

  it('displays timestamp', () => {
    const message: Message = {
      id: '1',
      role: 'user',
      content: 'Test',
      timestamp: new Date('2024-01-01T10:00:00Z'),
    };

    render(<MessageItem message={message} />);
    
    // Timestamp should be formatted and displayed in 12-hour format with AM/PM
    const timestamp = screen.getByText(/\d{1,2}:\d{2}\s?(AM|PM)/i);
    expect(timestamp).toBeInTheDocument();
  });

  it('preserves whitespace in user messages', () => {
    const message: Message = {
      id: '1',
      role: 'user',
      content: 'Line 1\nLine 2\nLine 3',
      timestamp: new Date('2024-01-01T10:00:00Z'),
    };

    const { container } = render(<MessageItem message={message} />);
    
    // User messages should use whitespace-pre-wrap
    const contentElement = container.querySelector('.whitespace-pre-wrap');
    expect(contentElement).toBeInTheDocument();
    expect(contentElement?.textContent).toContain('Line 1');
    expect(contentElement?.textContent).toContain('Line 2');
    expect(contentElement?.textContent).toContain('Line 3');
  });

  it('renders markdown in assistant messages', () => {
    const message: Message = {
      id: '2',
      role: 'assistant',
      content: '**Bold text** and *italic text*',
      timestamp: new Date('2024-01-01T10:00:01Z'),
    };

    render(<MessageItem message={message} />);
    
    // Markdown should be rendered
    expect(screen.getByText('Bold text')).toBeInTheDocument();
    expect(screen.getByText('and')).toBeInTheDocument();
    expect(screen.getByText('italic text')).toBeInTheDocument();
  });

  it('aligns user messages to the right', () => {
    const message: Message = {
      id: '1',
      role: 'user',
      content: 'Test',
      timestamp: new Date('2024-01-01T10:00:00Z'),
    };

    const { container } = render(<MessageItem message={message} />);
    
    // User messages should be right-aligned
    const wrapper = container.querySelector('.justify-end');
    expect(wrapper).toBeInTheDocument();
  });

  it('aligns assistant messages to the left', () => {
    const message: Message = {
      id: '2',
      role: 'assistant',
      content: 'Test',
      timestamp: new Date('2024-01-01T10:00:01Z'),
    };

    const { container } = render(<MessageItem message={message} />);
    
    // Assistant messages should be left-aligned
    const wrapper = container.querySelector('.justify-start');
    expect(wrapper).toBeInTheDocument();
  });
});
