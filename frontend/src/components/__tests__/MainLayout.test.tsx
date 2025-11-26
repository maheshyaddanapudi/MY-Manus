import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MainLayout } from '../Layout/MainLayout';

vi.mock('../../services/api');
vi.mock('../../services/websocket');

describe('MainLayout', () => {
  it('renders main layout', () => {
    render(<MainLayout />);
    expect(screen.getByText(/MY Manus/)).toBeInTheDocument();
  });

  it('renders all panel tabs', () => {
    render(<MainLayout />);
    expect(screen.getByText(/Events/)).toBeInTheDocument();
    expect(screen.getByText(/Browser/)).toBeInTheDocument();
    expect(screen.getByText(/Chat/)).toBeInTheDocument();
    expect(screen.getByText(/Terminal/)).toBeInTheDocument();
  });

  it('switches between panels', async () => {
    const user = userEvent.setup();
    render(<MainLayout />);

    // Click Browser tab
    await user.click(screen.getByText(/Browser/));
    expect(screen.getByTestId('browser-panel')).toBeInTheDocument();

    // Click Chat tab
    await user.click(screen.getByText(/Chat/));
    expect(screen.getByTestId('chat-panel')).toBeInTheDocument();
  });

  it('displays conversation list', () => {
    render(<MainLayout />);
    expect(screen.getByText(/Conversations/)).toBeInTheDocument();
  });

  it('creates new conversation', async () => {
    const user = userEvent.setup();
    render(<MainLayout />);

    const newButton = screen.getByText(/New Conversation/);
    await user.click(newButton);

    // Should create a new session
    expect(screen.getByText(/New Conversation/)).toBeInTheDocument();
  });
});
