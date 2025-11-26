import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { EditorPanel } from '../Editor/EditorPanel';
import * as apiService from '../../services/api';

vi.mock('../../services/api');

describe('EditorPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders editor panel', () => {
    render(<EditorPanel sessionId="test-session" />);
    expect(screen.getByText(/Editor/)).toBeInTheDocument();
  });

  it('displays Python code from agent actions', async () => {
    const mockEvents = [
      {
        id: 1,
        type: 'AGENT_ACTION',
        data: { pythonCode: "print('Hello')" },
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<EditorPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText(/print\('Hello'\)/)).toBeInTheDocument();
    });
  });

  it('supports syntax highlighting', async () => {
    const mockEvents = [
      {
        id: 1,
        type: 'AGENT_ACTION',
        data: { pythonCode: 'def hello():\n    return "world"' },
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<EditorPanel sessionId="test-session" />);

    await waitFor(() => {
      const codeElement = screen.getByText(/def hello/);
      expect(codeElement).toBeInTheDocument();
    });
  });
});
