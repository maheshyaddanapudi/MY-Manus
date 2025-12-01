import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ConversationList } from '../ConversationList/ConversationList';
import { useAgentStore } from '../../stores/agentStore';

// Mock agentStore
vi.mock('../../stores/agentStore');

// Mock Heroicons
vi.mock('@heroicons/react/24/outline', () => ({
  PlusIcon: () => <span>+</span>,
  TrashIcon: () => <span>🗑</span>,
  PencilIcon: () => <span>✏️</span>,
  CheckIcon: () => <span>✓</span>,
  XMarkIcon: () => <span>✗</span>,
}));

vi.mock('@heroicons/react/24/solid', () => ({
  ChatBubbleLeftIcon: () => <span>💬</span>,
}));

describe('ConversationList', () => {
  const mockSessions = [
    {
      sessionId: 'session-1',
      title: 'Conversation 1',
      createdAt: '2024-01-01T10:00:00Z',
      updatedAt: '2024-01-01T10:30:00Z',
      messageCount: 5,
    },
    {
      sessionId: 'session-2',
      title: 'Conversation 2',
      createdAt: '2024-01-02T10:00:00Z',
      updatedAt: '2024-01-02T10:30:00Z',
      messageCount: 3,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    
    // Default mock
    (useAgentStore as any).mockReturnValue({
      sessions: [],
      currentSessionId: null,
      loadSessions: vi.fn(),
      createNewSession: vi.fn(),
      switchSession: vi.fn(),
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });
  });

  it('renders New Chat button', () => {
    render(<ConversationList />);
    
    expect(screen.getByText('New Chat')).toBeInTheDocument();
  });

  it('calls createNewSession when clicking New Chat button', async () => {
    const mockCreateNewSession = vi.fn();
    const user = userEvent.setup();

    (useAgentStore as any).mockReturnValue({
      sessions: [],
      currentSessionId: null,
      loadSessions: vi.fn(),
      createNewSession: mockCreateNewSession,
      switchSession: vi.fn(),
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    const newButton = screen.getByText('New Chat');
    await user.click(newButton);

    expect(mockCreateNewSession).toHaveBeenCalled();
  });

  it('displays empty state when no conversations', () => {
    (useAgentStore as any).mockReturnValue({
      sessions: [],
      currentSessionId: null,
      loadSessions: vi.fn(),
      createNewSession: vi.fn(),
      switchSession: vi.fn(),
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    expect(screen.getByText('No conversations yet')).toBeInTheDocument();
    expect(screen.getByText('Click "New Chat" to start')).toBeInTheDocument();
  });

  it('displays list of conversations', () => {
    (useAgentStore as any).mockReturnValue({
      sessions: mockSessions,
      currentSessionId: null,
      loadSessions: vi.fn(),
      createNewSession: vi.fn(),
      switchSession: vi.fn(),
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    expect(screen.getByText('Conversation 1')).toBeInTheDocument();
    expect(screen.getByText('Conversation 2')).toBeInTheDocument();
  });

  it('highlights current session', () => {
    (useAgentStore as any).mockReturnValue({
      sessions: mockSessions,
      currentSessionId: 'session-1',
      loadSessions: vi.fn(),
      createNewSession: vi.fn(),
      switchSession: vi.fn(),
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    // Find the parent container div (not the inner text div)
    const session1Container = screen.getByText('Conversation 1').closest('.group');
    expect(session1Container).toHaveClass('bg-gray-800');
    expect(session1Container).toHaveClass('text-white');
  });

  it('calls switchSession when clicking a conversation', async () => {
    const mockSwitchSession = vi.fn();
    const user = userEvent.setup();

    (useAgentStore as any).mockReturnValue({
      sessions: mockSessions,
      currentSessionId: null,
      loadSessions: vi.fn(),
      createNewSession: vi.fn(),
      switchSession: mockSwitchSession,
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    await user.click(screen.getByText('Conversation 1'));

    expect(mockSwitchSession).toHaveBeenCalledWith('session-1');
  });

  it('does not call switchSession when clicking current session', async () => {
    const mockSwitchSession = vi.fn();
    const user = userEvent.setup();

    (useAgentStore as any).mockReturnValue({
      sessions: mockSessions,
      currentSessionId: 'session-1',
      loadSessions: vi.fn(),
      createNewSession: vi.fn(),
      switchSession: mockSwitchSession,
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    await user.click(screen.getByText('Conversation 1'));

    expect(mockSwitchSession).not.toHaveBeenCalled();
  });

  it('displays message count for each conversation', () => {
    (useAgentStore as any).mockReturnValue({
      sessions: mockSessions,
      currentSessionId: null,
      loadSessions: vi.fn(),
      createNewSession: vi.fn(),
      switchSession: vi.fn(),
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    expect(screen.getByText(/5 messages/)).toBeInTheDocument();
    expect(screen.getByText(/3 messages/)).toBeInTheDocument();
  });

  it('calls loadSessions on mount', () => {
    const mockLoadSessions = vi.fn();

    (useAgentStore as any).mockReturnValue({
      sessions: [],
      currentSessionId: null,
      loadSessions: mockLoadSessions,
      createNewSession: vi.fn(),
      switchSession: vi.fn(),
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    expect(mockLoadSessions).toHaveBeenCalled();
  });

  it('shows Creating... when creating new session', async () => {
    const mockCreateNewSession = vi.fn(() => new Promise(resolve => setTimeout(resolve, 100)));
    const user = userEvent.setup();

    (useAgentStore as any).mockReturnValue({
      sessions: [],
      currentSessionId: null,
      loadSessions: vi.fn(),
      createNewSession: mockCreateNewSession,
      switchSession: vi.fn(),
      deleteSession: vi.fn(),
      renameSession: vi.fn(),
    });

    render(<ConversationList />);

    const newButton = screen.getByText('New Chat');
    await user.click(newButton);

    expect(screen.getByText('Creating...')).toBeInTheDocument();
  });
});
