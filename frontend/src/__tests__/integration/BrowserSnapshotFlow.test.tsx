import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserPanel } from '../../components/Browser/BrowserPanel';
import * as apiService from '../../services/api';

vi.mock('../../services/api');

describe('Browser Snapshot Flow Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('displays browser snapshots from tool executions', async () => {
    const mockToolExecutions = [
      {
        id: 1,
        toolName: 'browser_view',
        result: {
          screenshot: 'base64screenshot1',
          url: 'https://example.com',
          title: 'Example Domain',
          htmlContent: '<html>...</html>',
          accessibilityTree: 'RootWebArea',
          timestamp: Date.now(),
        },
        timestamp: new Date(),
      },
      {
        id: 2,
        toolName: 'browser_view',
        result: {
          screenshot: 'base64screenshot2',
          url: 'https://github.com',
          title: 'GitHub',
          htmlContent: '<html>...</html>',
          accessibilityTree: 'RootWebArea',
          timestamp: Date.now(),
        },
        timestamp: new Date(),
      },
    ];

    (apiService.getToolExecutions as any).mockResolvedValue(mockToolExecutions);

    render(<BrowserPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText(/Example Domain/)).toBeInTheDocument();
      expect(screen.getByText(/GitHub/)).toBeInTheDocument();
    });
  });

  it('switches between snapshot view modes', async () => {
    const user = userEvent.setup();

    const mockToolExecutions = [
      {
        id: 1,
        toolName: 'browser_view',
        result: {
          screenshot: 'base64screenshot',
          url: 'https://example.com',
          title: 'Example',
          htmlContent: '<html><body>Test Content</body></html>',
          accessibilityTree: 'RootWebArea "Example" [web area]',
          timestamp: Date.now(),
        },
        timestamp: new Date(),
      },
    ];

    (apiService.getToolExecutions as any).mockResolvedValue(mockToolExecutions);

    render(<BrowserPanel sessionId="test-session" />);

    // Wait for snapshot to load
    await waitFor(() => {
      expect(screen.getByText(/Example/)).toBeInTheDocument();
    });

    // Click HTML view
    const htmlButton = screen.getByText(/HTML/);
    await user.click(htmlButton);

    await waitFor(() => {
      expect(screen.getByText(/Test Content/)).toBeInTheDocument();
    });

    // Click Accessibility Tree view
    const treeButton = screen.getByText(/Tree/);
    await user.click(treeButton);

    await waitFor(() => {
      expect(screen.getByText(/RootWebArea/)).toBeInTheDocument();
    });
  });

  it('filters snapshots by iteration', async () => {
    const mockToolExecutions = Array.from({ length: 10 }, (_, i) => ({
      id: i + 1,
      toolName: 'browser_view',
      iteration: Math.floor(i / 2) + 1,
      result: {
        screenshot: `base64screenshot${i}`,
        url: `https://example${i}.com`,
        title: `Page ${i}`,
        timestamp: Date.now() - i * 1000,
      },
      timestamp: new Date(),
    }));

    (apiService.getToolExecutions as any).mockResolvedValue(mockToolExecutions);

    render(<BrowserPanel sessionId="test-session" />);

    await waitFor(() => {
      expect(screen.getByText(/Page 0/)).toBeInTheDocument();
    });
  });
});
