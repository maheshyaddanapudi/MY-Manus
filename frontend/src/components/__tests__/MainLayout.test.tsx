import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MainLayout } from '../Layout/MainLayout';
import { useAgentStore } from '../../stores/agentStore';

// Mock agentStore
vi.mock('../../stores/agentStore');

// Mock child components to simplify testing
vi.mock('../EventStream', () => ({
  EventStreamPanel: () => <div data-testid="events-panel">Event Stream Panel</div>,
}));

vi.mock('../Browser', () => ({
  EnhancedBrowserPanel: () => <div data-testid="browser-panel">Browser Panel</div>,
}));

vi.mock('../FileTree', () => ({
  FileTreePanel: () => <div data-testid="files-panel">File Tree Panel</div>,
}));

vi.mock('../Replay', () => ({
  SessionReplayPanel: () => <div data-testid="replay-panel">Replay Panel</div>,
}));

vi.mock('../Knowledge', () => ({
  DocumentPanel: () => <div data-testid="knowledge-panel">Knowledge Panel</div>,
}));

vi.mock('../Plan', () => ({
  PlanPanel: () => <div data-testid="plan-panel">Plan Panel</div>,
}));

vi.mock('../Layout/Header', () => ({
  Header: () => <div>MY Manus Header</div>,
}));

describe('MainLayout', () => {
  const mockConversationListPanel = <div data-testid="conversation-list">Conversation List</div>;
  const mockChatPanel = <div data-testid="chat-panel">Chat Panel</div>;
  const mockTerminalPanel = <div data-testid="terminal-panel">Terminal Panel</div>;
  const mockEditorPanel = <div data-testid="editor-panel">Editor Panel</div>;

  beforeEach(() => {
    vi.clearAllMocks();
    
    // Default mock for useAgentStore
    (useAgentStore as any).mockReturnValue({
      activePanel: 'terminal',
      currentSessionId: 'test-session-123',
    });
  });

  it('renders main layout with header', () => {
    render(
      <MainLayout
        conversationListPanel={mockConversationListPanel}
        chatPanel={mockChatPanel}
        terminalPanel={mockTerminalPanel}
        editorPanel={mockEditorPanel}
      />
    );

    expect(screen.getByText('MY Manus Header')).toBeInTheDocument();
  });

  it('renders all provided panels', () => {
    render(
      <MainLayout
        conversationListPanel={mockConversationListPanel}
        chatPanel={mockChatPanel}
        terminalPanel={mockTerminalPanel}
        editorPanel={mockEditorPanel}
      />
    );

    expect(screen.getByTestId('conversation-list')).toBeInTheDocument();
    expect(screen.getByTestId('chat-panel')).toBeInTheDocument();
    expect(screen.getByTestId('terminal-panel')).toBeInTheDocument();
  });

  it('renders all panel tabs', () => {
    render(
      <MainLayout
        conversationListPanel={mockConversationListPanel}
        chatPanel={mockChatPanel}
        terminalPanel={mockTerminalPanel}
        editorPanel={mockEditorPanel}
      />
    );

    // Check for actual tab labels from the component
    expect(screen.getByText('Terminal')).toBeInTheDocument();
    expect(screen.getByText('Code Editor')).toBeInTheDocument();
    expect(screen.getByText('Event Stream')).toBeInTheDocument();
    expect(screen.getByText('Browser')).toBeInTheDocument();
    expect(screen.getByText('Files')).toBeInTheDocument();
    expect(screen.getByText('Replay')).toBeInTheDocument();
    expect(screen.getByText('Knowledge')).toBeInTheDocument();
    expect(screen.getByText('Plan')).toBeInTheDocument();
  });

  it('displays active panel based on store state - terminal', () => {
    (useAgentStore as any).mockReturnValue({
      activePanel: 'terminal',
      currentSessionId: 'test-session-123',
    });

    render(
      <MainLayout
        conversationListPanel={mockConversationListPanel}
        chatPanel={mockChatPanel}
        terminalPanel={mockTerminalPanel}
        editorPanel={mockEditorPanel}
      />
    );

    expect(screen.getByTestId('terminal-panel')).toBeInTheDocument();
  });

  it('displays active panel based on store state - editor', () => {
    (useAgentStore as any).mockReturnValue({
      activePanel: 'editor',
      currentSessionId: 'test-session-123',
    });

    render(
      <MainLayout
        conversationListPanel={mockConversationListPanel}
        chatPanel={mockChatPanel}
        terminalPanel={mockTerminalPanel}
        editorPanel={mockEditorPanel}
      />
    );

    expect(screen.getByTestId('editor-panel')).toBeInTheDocument();
  });

  it('displays events panel when active', () => {
    (useAgentStore as any).mockReturnValue({
      activePanel: 'events',
      currentSessionId: 'test-session-123',
    });

    render(
      <MainLayout
        conversationListPanel={mockConversationListPanel}
        chatPanel={mockChatPanel}
        terminalPanel={mockTerminalPanel}
        editorPanel={mockEditorPanel}
      />
    );

    expect(screen.getByTestId('events-panel')).toBeInTheDocument();
  });

  it('displays browser panel when active', () => {
    (useAgentStore as any).mockReturnValue({
      activePanel: 'browser',
      currentSessionId: 'test-session-123',
    });

    render(
      <MainLayout
        conversationListPanel={mockConversationListPanel}
        chatPanel={mockChatPanel}
        terminalPanel={mockTerminalPanel}
        editorPanel={mockEditorPanel}
      />
    );

    expect(screen.getByTestId('browser-panel')).toBeInTheDocument();
  });

  it('renders without conversation list panel when not provided', () => {
    render(
      <MainLayout
        chatPanel={mockChatPanel}
        terminalPanel={mockTerminalPanel}
        editorPanel={mockEditorPanel}
      />
    );

    expect(screen.queryByTestId('conversation-list')).not.toBeInTheDocument();
    expect(screen.getByTestId('chat-panel')).toBeInTheDocument();
  });
});
