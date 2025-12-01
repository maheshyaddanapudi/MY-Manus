import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EventItem from '../EventStream/EventItem';
import type { Event } from '../../types';

describe('EventItem', () => {
  it('renders user message event', () => {
    const event: Event = {
      id: 'event-1',
      type: 'USER_MESSAGE',
      iteration: 1,
      sequence: 1,
      content: 'Hello, agent!',
      timestamp: '2024-01-01T10:00:00Z',
    };

    render(<EventItem event={event} />);
    
    expect(screen.getByText('User Message')).toBeInTheDocument();
    expect(screen.getByText(/Hello, agent!/)).toBeInTheDocument();
    expect(screen.getByText('Iteration 1')).toBeInTheDocument();
  });

  it('renders agent thought event', () => {
    const event: Event = {
      id: 'event-2',
      type: 'AGENT_THOUGHT',
      iteration: 1,
      sequence: 2,
      content: 'I need to read the file',
      timestamp: '2024-01-01T10:00:01Z',
    };

    render(<EventItem event={event} />);
    
    expect(screen.getByText('Agent Thought')).toBeInTheDocument();
    expect(screen.getByText(/I need to read the file/)).toBeInTheDocument();
  });

  it('renders agent action event', () => {
    const event: Event = {
      id: 'event-3',
      type: 'AGENT_ACTION',
      iteration: 1,
      sequence: 3,
      content: "print('Hello')",
      timestamp: '2024-01-01T10:00:02Z',
      durationMs: 150,
    };

    render(<EventItem event={event} />);
    
    expect(screen.getByText('Agent Action')).toBeInTheDocument();
    expect(screen.getByText(/print\('Hello'\)/)).toBeInTheDocument();
    expect(screen.getByText('150ms')).toBeInTheDocument();
  });

  it('renders observation event with success', () => {
    const event: Event = {
      id: 'event-4',
      type: 'OBSERVATION',
      iteration: 1,
      sequence: 4,
      content: 'Command output',
      timestamp: '2024-01-01T10:00:03Z',
      success: true,
    };

    render(<EventItem event={event} />);
    
    expect(screen.getByText('Observation')).toBeInTheDocument();
    expect(screen.getByText(/Command output/)).toBeInTheDocument();
    expect(screen.getByText('✅')).toBeInTheDocument();
  });

  it('renders observation event with error', () => {
    const event: Event = {
      id: 'event-5',
      type: 'OBSERVATION',
      iteration: 1,
      sequence: 5,
      content: 'Command failed',
      timestamp: '2024-01-01T10:00:04Z',
      success: false,
      error: 'File not found',
    };

    render(<EventItem event={event} />);
    
    expect(screen.getByText('Observation')).toBeInTheDocument();
    expect(screen.getByText(/Command failed/)).toBeInTheDocument();
    expect(screen.getByText('❌')).toBeInTheDocument();
  });

  it('expands to show full content when clicked', async () => {
    const user = userEvent.setup();
    const longContent = 'This is a very long content that should be truncated in the preview but shown in full when expanded';
    
    const event: Event = {
      id: 'event-6',
      type: 'AGENT_THOUGHT',
      iteration: 1,
      sequence: 6,
      content: longContent,
      timestamp: '2024-01-01T10:00:05Z',
    };

    render(<EventItem event={event} />);
    
    // Initially shows truncated content
    expect(screen.getByText(/This is a very long content/)).toBeInTheDocument();
    
    // Click to expand
    await user.click(screen.getByText('Agent Thought'));
    
    // Now shows full content in expanded view
    expect(screen.getByText('Content:')).toBeInTheDocument();
    expect(screen.getByText(longContent)).toBeInTheDocument();
  });

  it('displays error message when present', async () => {
    const user = userEvent.setup();
    
    const event: Event = {
      id: 'event-7',
      type: 'ERROR',
      iteration: 1,
      sequence: 7,
      content: 'Operation failed',
      timestamp: '2024-01-01T10:00:06Z',
      error: 'Network timeout',
    };

    render(<EventItem event={event} />);
    
    // Click to expand
    await user.click(screen.getByText('Error'));
    
    // Error should be visible
    expect(screen.getByText('Error:')).toBeInTheDocument();
    expect(screen.getByText('Network timeout')).toBeInTheDocument();
  });

  it('displays additional data when present', async () => {
    const user = userEvent.setup();
    
    const event: Event = {
      id: 'event-8',
      type: 'AGENT_ACTION',
      iteration: 1,
      sequence: 8,
      content: 'Execute command',
      timestamp: '2024-01-01T10:00:07Z',
      data: {
        tool: 'shell_exec',
        command: 'ls -la',
      },
    };

    render(<EventItem event={event} />);
    
    // Click to expand
    await user.click(screen.getByText('Agent Action'));
    
    // Data should be visible
    expect(screen.getByText('Metadata:')).toBeInTheDocument();
  });

  it('displays timestamp', () => {
    const event: Event = {
      id: 'event-9',
      type: 'SYSTEM',
      iteration: 1,
      sequence: 9,
      content: 'System message',
      timestamp: '2024-01-01T10:00:00Z',
    };

    render(<EventItem event={event} />);
    
    // Timestamp should be formatted and displayed
    const timestamp = screen.getByText(/\d{1,2}:\d{2}:\d{2}/);
    expect(timestamp).toBeInTheDocument();
  });
});
