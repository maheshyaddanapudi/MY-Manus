import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserPanel } from '../Browser/BrowserPanel';
import { apiService } from '../../services/api';

// Mock apiService
vi.mock('../../services/api', () => ({
  apiService: {
    getToolExecutions: vi.fn(),
  },
}));

// Mock SnapshotViewer to isolate BrowserPanel testing
vi.mock('../Browser/SnapshotViewer', () => ({
  SnapshotViewer: ({ snapshot, viewMode }: any) => (
    <div data-testid="snapshot-viewer">
      <div>Snapshot: {snapshot.title}</div>
      <div>URL: {snapshot.url}</div>
      <div>View Mode: {viewMode}</div>
    </div>
  ),
}));

describe('BrowserPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders browser panel', () => {
    (apiService.getToolExecutions as any).mockResolvedValue([]);
    
    render(<BrowserPanel sessionId="test-session" />);
    expect(screen.getByText(/Browser/)).toBeInTheDocument();
  });

  it('fetches and displays snapshots', async () => {
    const mockToolExecutions = [
      {
        id: 1,
        toolName: 'browser_view',
        timestamp: '2024-01-01T10:00:00Z',
        result: {
          screenshot: 'base64image',
          url: 'https://example.com',
          title: 'Example Page',
          timestamp: Date.now(),
        },
      },
    ];

    (apiService.getToolExecutions as any).mockResolvedValue(mockToolExecutions);

    render(<BrowserPanel sessionId="test-session" />);

    // Wait for API call
    await waitFor(() => {
      expect(apiService.getToolExecutions).toHaveBeenCalledWith('test-session');
    });

    // Wait for snapshot to be displayed
    await waitFor(() => {
      expect(screen.getByText('1 snapshot')).toBeInTheDocument();
    });

    // Snapshot viewer should be rendered with the snapshot
    await waitFor(() => {
      expect(screen.getByTestId('snapshot-viewer')).toBeInTheDocument();
      expect(screen.getByText('Snapshot: Example Page')).toBeInTheDocument();
      expect(screen.getByText('URL: https://example.com')).toBeInTheDocument();
    });
  });

  it('displays empty state when no snapshots', async () => {
    (apiService.getToolExecutions as any).mockResolvedValue([]);

    render(<BrowserPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(apiService.getToolExecutions).toHaveBeenCalledWith('test-session');
    });

    await waitFor(() => {
      expect(screen.getByText('No browser snapshots yet')).toBeInTheDocument();
    });

    expect(screen.getByText(/Browser snapshots will appear here/)).toBeInTheDocument();
  });

  it('filters only browser_view tool executions', async () => {
    const mockToolExecutions = [
      {
        id: 1,
        toolName: 'browser_view',
        timestamp: '2024-01-01T10:00:00Z',
        result: {
          screenshot: 'base64image1',
          url: 'https://example.com',
          title: 'Example',
          timestamp: Date.now(),
        },
      },
      {
        id: 2,
        toolName: 'shell_exec', // Should be filtered out
        timestamp: '2024-01-01T10:00:01Z',
        result: {
          stdout: 'output',
        },
      },
      {
        id: 3,
        toolName: 'browser_view',
        timestamp: '2024-01-01T10:00:02Z',
        result: {
          screenshot: 'base64image2',
          url: 'https://example2.com',
          title: 'Example 2',
          timestamp: Date.now(),
        },
      },
    ];

    (apiService.getToolExecutions as any).mockResolvedValue(mockToolExecutions);

    render(<BrowserPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText('2 snapshots')).toBeInTheDocument();
    });
  });

  it('handles API errors gracefully', async () => {
    (apiService.getToolExecutions as any).mockRejectedValue(new Error('API Error'));

    render(<BrowserPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText(/API Error/)).toBeInTheDocument();
    });
  });

  it('does not fetch when sessionId is empty', () => {
    (apiService.getToolExecutions as any).mockResolvedValue([]);

    render(<BrowserPanel sessionId="" />);

    // Should not call API with empty sessionId
    expect(apiService.getToolExecutions).not.toHaveBeenCalled();
  });

  it('displays snapshot count', async () => {
    const mockToolExecutions = [
      {
        id: 1,
        toolName: 'browser_view',
        timestamp: '2024-01-01T10:00:00Z',
        result: { screenshot: 'img1', url: 'url1', title: 'Title 1', timestamp: Date.now() },
      },
      {
        id: 2,
        toolName: 'browser_view',
        timestamp: '2024-01-01T10:00:01Z',
        result: { screenshot: 'img2', url: 'url2', title: 'Title 2', timestamp: Date.now() },
      },
      {
        id: 3,
        toolName: 'browser_view',
        timestamp: '2024-01-01T10:00:02Z',
        result: { screenshot: 'img3', url: 'url3', title: 'Title 3', timestamp: Date.now() },
      },
    ];

    (apiService.getToolExecutions as any).mockResolvedValue(mockToolExecutions);

    render(<BrowserPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText('3 snapshots')).toBeInTheDocument();
    });
  });
});
