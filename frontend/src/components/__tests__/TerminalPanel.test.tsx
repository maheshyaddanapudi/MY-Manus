import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { TerminalPanel } from '../Terminal/TerminalPanel';
import * as apiService from '../../services/api';

vi.mock('../../services/api');

describe('TerminalPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders terminal panel', () => {
    render(<TerminalPanel sessionId="test-session" />);
    expect(screen.getByText(/Terminal/)).toBeInTheDocument();
  });

  it('displays code execution output', async () => {
    const mockEvents = [
      {
        id: 1,
        type: 'OBSERVATION',
        data: { stdout: 'Hello, World!', exitCode: 0 },
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<TerminalPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText(/Hello, World!/)).toBeInTheDocument();
    });
  });

  it('displays error messages', async () => {
    const mockEvents = [
      {
        id: 1,
        type: 'OBSERVATION',
        data: { stderr: 'Error occurred', exitCode: 1 },
      },
    ];

    (apiService.getEventStream as any).mockResolvedValue(mockEvents);

    render(<TerminalPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText(/Error occurred/)).toBeInTheDocument();
    });
  });
});
