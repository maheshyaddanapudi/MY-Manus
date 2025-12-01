import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Header } from '../Layout/Header';
import { useAgentStore } from '../../stores/agentStore';

// Mock agentStore
vi.mock('../../stores/agentStore');

// Mock NotificationBell to avoid API calls
vi.mock('../Notifications', () => ({
  NotificationBell: () => <div data-testid="notification-bell">Notifications</div>,
}));

describe('Header', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    
    // Default mock for useAgentStore
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123',
      isConnected: true,
      agentStatus: 'idle',
    });
  });

  it('renders header with title', () => {
    render(<Header />);
    expect(screen.getByText('MY Manus')).toBeInTheDocument();
    expect(screen.getByText('CodeAct AI Agent')).toBeInTheDocument();
  });

  it('displays session ID when available', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123456789',
      isConnected: true,
      agentStatus: 'idle',
    });

    render(<Header />);
    expect(screen.getByText(/Session: test-ses.../)).toBeInTheDocument();
  });

  it('displays connection status - connected', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123',
      isConnected: true,
      agentStatus: 'idle',
    });

    render(<Header />);
    expect(screen.getByText('Idle')).toBeInTheDocument();
  });

  it('displays connection status - disconnected', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: null,
      isConnected: false,
      agentStatus: 'idle',
    });

    render(<Header />);
    expect(screen.getByText('Disconnected')).toBeInTheDocument();
  });

  it('displays agent status - thinking', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123',
      isConnected: true,
      agentStatus: 'thinking',
    });

    render(<Header />);
    expect(screen.getByText('Thinking')).toBeInTheDocument();
  });

  it('displays agent status - executing', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123',
      isConnected: true,
      agentStatus: 'executing',
    });

    render(<Header />);
    expect(screen.getByText('Executing')).toBeInTheDocument();
  });

  it('displays agent status - error', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123',
      isConnected: true,
      agentStatus: 'error',
    });

    render(<Header />);
    expect(screen.getByText('Error')).toBeInTheDocument();
  });

  it('displays agent status - done', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: 'test-session-123',
      isConnected: true,
      agentStatus: 'done',
    });

    render(<Header />);
    expect(screen.getByText('Done')).toBeInTheDocument();
  });

  it('renders notification bell', () => {
    render(<Header />);
    expect(screen.getByTestId('notification-bell')).toBeInTheDocument();
  });

  it('hides session ID when not available', () => {
    (useAgentStore as any).mockReturnValue({
      sessionId: null,
      isConnected: false,
      agentStatus: 'idle',
    });

    render(<Header />);
    expect(screen.queryByText(/Session:/)).not.toBeInTheDocument();
  });
});
