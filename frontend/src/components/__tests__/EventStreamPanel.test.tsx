import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import EventStreamPanel from '../EventStream/EventStreamPanel';
import { useAgentStore } from '../../stores/agentStore';

// Mock agentStore
vi.mock('../../stores/agentStore');

// Mock EventItem component to simplify testing
vi.mock('../EventStream/EventItem', () => ({
  default: ({ event }: any) => (
    <div data-testid={`event-${event.id}`}>
      {event.type}: {event.content}
    </div>
  ),
}));

/**
 * Test Suite for EventStreamPanel Component
 * 
 * Tests the store-based EventStreamPanel that:
 * - Reads events from agentStore
 * - Filters events by iteration
 * - Displays event count
 * - Shows empty state when no events
 */
describe('EventStreamPanel', () => {
  const mockSessionId = 'test-session-123';

  const mockEvents = [
    {
      id: 'event-1',
      type: 'ITERATION_START',
      iteration: 1,
      content: 'Starting iteration 1',
      timestamp: '2024-01-01T10:00:00Z',
    },
    {
      id: 'event-2',
      type: 'AGENT_THOUGHT',
      iteration: 1,
      content: 'Thinking about the task',
      timestamp: '2024-01-01T10:00:01Z',
    },
    {
      id: 'event-3',
      type: 'ITERATION_START',
      iteration: 2,
      content: 'Starting iteration 2',
      timestamp: '2024-01-01T10:01:00Z',
    },
    {
      id: 'event-4',
      type: 'AGENT_ACTION',
      iteration: 2,
      content: 'Executing action',
      timestamp: '2024-01-01T10:01:01Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders without crashing', () => {
    (useAgentStore as any).mockReturnValue({ events: [] });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    expect(screen.getByText('Event Stream')).toBeInTheDocument();
  });

  it('displays events from store', () => {
    (useAgentStore as any).mockReturnValue({ events: mockEvents });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    // Check that all events are rendered
    expect(screen.getByTestId('event-event-1')).toBeInTheDocument();
    expect(screen.getByTestId('event-event-2')).toBeInTheDocument();
    expect(screen.getByTestId('event-event-3')).toBeInTheDocument();
    expect(screen.getByTestId('event-event-4')).toBeInTheDocument();
  });

  it('displays event count', () => {
    (useAgentStore as any).mockReturnValue({ events: mockEvents });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    expect(screen.getByText('4 events')).toBeInTheDocument();
  });

  it('handles empty event stream', () => {
    (useAgentStore as any).mockReturnValue({ events: [] });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    expect(screen.getByText('No Events Yet')).toBeInTheDocument();
    expect(screen.getByText('0 events')).toBeInTheDocument();
  });

  it('filters events by iteration', () => {
    (useAgentStore as any).mockReturnValue({ events: mockEvents });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    // Initially shows all events
    expect(screen.getByTestId('event-event-1')).toBeInTheDocument();
    expect(screen.getByTestId('event-event-3')).toBeInTheDocument();

    // Find and click the iteration filter dropdown
    const select = screen.getByRole('combobox');
    
    // Filter to iteration 1
    fireEvent.change(select, { target: { value: '1' } });

    // Should show only iteration 1 events
    expect(screen.getByTestId('event-event-1')).toBeInTheDocument();
    expect(screen.getByTestId('event-event-2')).toBeInTheDocument();
    expect(screen.queryByTestId('event-event-3')).not.toBeInTheDocument();
    expect(screen.queryByTestId('event-event-4')).not.toBeInTheDocument();

    // Event count should update
    expect(screen.getByText('2 events')).toBeInTheDocument();
  });

  it('shows iteration dropdown with correct options', () => {
    (useAgentStore as any).mockReturnValue({ events: mockEvents });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    const select = screen.getByRole('combobox');
    
    // Should have "All" option and iteration options
    expect(screen.getByText(/🔄 All Iterations/)).toBeInTheDocument();
    expect(screen.getByText(/#1 Iteration 1/)).toBeInTheDocument();
    expect(screen.getByText(/#2 Iteration 2/)).toBeInTheDocument();
  });

  it('displays event count in footer', () => {
    (useAgentStore as any).mockReturnValue({ events: mockEvents });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    // Should show event count in footer
    expect(screen.getByText(/4 events/)).toBeInTheDocument();
  });

  it('displays iteration count in footer', () => {
    (useAgentStore as any).mockReturnValue({ events: mockEvents });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    // Should show 2 iterations (text is split across elements)
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('iterations')).toBeInTheDocument();
  });

  it('resets to all iterations when selecting "All"', () => {
    (useAgentStore as any).mockReturnValue({ events: mockEvents });

    render(<EventStreamPanel sessionId={mockSessionId} />);

    const select = screen.getByRole('combobox');
    
    // Filter to iteration 1
    fireEvent.change(select, { target: { value: '1' } });
    expect(screen.getByText('2 events')).toBeInTheDocument();

    // Reset to all
    fireEvent.change(select, { target: { value: 'all' } });
    expect(screen.getByText('4 events')).toBeInTheDocument();
  });
});
