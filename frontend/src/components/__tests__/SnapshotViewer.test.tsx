import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SnapshotViewer } from '../Browser/SnapshotViewer';

describe('SnapshotViewer', () => {
  const mockSnapshot = {
    id: '1',
    timestamp: Date.now(),
    screenshot: 'base64screenshot',
    url: 'https://example.com',
    title: 'Example Domain',
    htmlContent: '<html><body>Test</body></html>',
    accessibilityTree: 'RootWebArea',
  };

  it('renders snapshot viewer', () => {
    render(<SnapshotViewer snapshot={mockSnapshot} />);
    expect(screen.getByText(/Example Domain/)).toBeInTheDocument();
  });

  it('displays screenshot by default', () => {
    render(<SnapshotViewer snapshot={mockSnapshot} />);
    const img = screen.getByAltText(/Example Domain/);
    expect(img).toBeInTheDocument();
  });

  it('switches to HTML view', async () => {
    const user = userEvent.setup();
    render(<SnapshotViewer snapshot={mockSnapshot} />);

    const htmlButton = screen.getByText(/HTML/);
    await user.click(htmlButton);

    expect(screen.getByText(/<html><body>Test<\/body><\/html>/)).toBeInTheDocument();
  });

  it('switches to accessibility tree view', async () => {
    const user = userEvent.setup();
    render(<SnapshotViewer snapshot={mockSnapshot} />);

    const treeButton = screen.getByText(/Tree/);
    await user.click(treeButton);

    expect(screen.getByText(/RootWebArea/)).toBeInTheDocument();
  });
});
