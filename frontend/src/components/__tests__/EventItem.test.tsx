import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { EventItem } from '../EventStream/EventItem';

describe('EventItem', () => {
  it('renders user message event', () => {
    const event = {
      id: 1,
      sessionId: 'test-session',
      type: 'USER_MESSAGE',
      sequence: 1,
      data: { text: 'Hello, agent!' },
      timestamp: new Date(),
    };

    render(<EventItem event={event} />);
    expect(screen.getByText(/Hello, agent!/)).toBeInTheDocument();
  });

  it('renders agent thought event', () => {
    const event = {
      id: 2,
      sessionId: 'test-session',
      type: 'AGENT_THOUGHT',
      sequence: 2,
      data: { thought: 'I need to read the file' },
      timestamp: new Date(),
    };

    render(<EventItem event={event} />);
    expect(screen.getByText(/I need to read the file/)).toBeInTheDocument();
  });

  it('renders agent action event with code', () => {
    const event = {
      id: 3,
      sessionId: 'test-session',
      type: 'AGENT_ACTION',
      sequence: 3,
      data: { pythonCode: "print('Hello')" },
      timestamp: new Date(),
    };

    render(<EventItem event={event} />);
    expect(screen.getByText(/print\('Hello'\)/)).toBeInTheDocument();
  });

  it('renders observation event', () => {
    const event = {
      id: 4,
      sessionId: 'test-session',
      type: 'OBSERVATION',
      sequence: 4,
      data: { stdout: 'Command output', exitCode: 0 },
      timestamp: new Date(),
    };

    render(<EventItem event={event} />);
    expect(screen.getByText(/Command output/)).toBeInTheDocument();
  });
});
