import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserPanel } from '../Browser/BrowserPanel';
import * as apiService from '../../services/api';

vi.mock('../../services/api');

describe('BrowserPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders browser panel', () => {
    render(<BrowserPanel sessionId="test-session" />);
    expect(screen.getByText(/Browser/)).toBeInTheDocument();
  });

  it('fetches and displays snapshots', async () => {
    const mockSnapshots = [
      {
        id: '1',
        timestamp: Date.now(),
        screenshot: 'base64image',
        url: 'https://example.com',
        title: 'Example',
      },
    ];

    (apiService.getToolExecutions as any).mockResolvedValue([
      {
        id: 1,
        toolName: 'browser_view',
        result: {
          screenshot: 'base64image',
          url: 'https://example.com',
          title: 'Example',
          timestamp: Date.now(),
        },
      },
    ]);

    render(<BrowserPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(apiService.getToolExecutions).toHaveBeenCalledWith('test-session');
    });
  });

  it('displays empty state when no snapshots', async () => {
    (apiService.getToolExecutions as any).mockResolvedValue([]);

    render(<BrowserPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText(/No browser snapshots/)).toBeInTheDocument();
    });
  });
});
