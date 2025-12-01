import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SnapshotViewer } from '../Browser/SnapshotViewer';

describe('SnapshotViewer', () => {
  const mockSnapshot = {
    screenshot: 'base64screenshot',
    url: 'https://example.com',
    title: 'Example Domain',
    timestamp: Date.now(),
    htmlContent: '<html><body>Test</body></html>',
    accessibilityTree: 'RootWebArea\n  button "Click me"',
  };

  it('renders snapshot info (title and URL)', () => {
    render(<SnapshotViewer snapshot={mockSnapshot} viewMode="screenshot" />);
    
    expect(screen.getByText('Example Domain')).toBeInTheDocument();
    expect(screen.getByText('https://example.com')).toBeInTheDocument();
  });

  it('displays timestamp', () => {
    const timestamp = new Date('2024-01-01T12:00:00Z').getTime();
    const snapshotWithTimestamp = { ...mockSnapshot, timestamp };
    
    render(<SnapshotViewer snapshot={snapshotWithTimestamp} viewMode="screenshot" />);
    
    // Timestamp should be displayed (exact format depends on locale)
    expect(screen.getByText(/2024|1\/1\/2024/)).toBeInTheDocument();
  });

  describe('Screenshot view mode', () => {
    it('displays screenshot when viewMode is "screenshot"', () => {
      render(<SnapshotViewer snapshot={mockSnapshot} viewMode="screenshot" />);
      
      const img = screen.getByAltText('Example Domain');
      expect(img).toBeInTheDocument();
      expect(img).toHaveAttribute('src', 'data:image/png;base64,base64screenshot');
    });

    it('does not display HTML or tree content in screenshot mode', () => {
      render(<SnapshotViewer snapshot={mockSnapshot} viewMode="screenshot" />);
      
      expect(screen.queryByText('<html><body>Test</body></html>')).not.toBeInTheDocument();
      expect(screen.queryByText(/RootWebArea/)).not.toBeInTheDocument();
    });
  });

  describe('HTML view mode', () => {
    it('displays HTML content when viewMode is "html"', () => {
      render(<SnapshotViewer snapshot={mockSnapshot} viewMode="html" />);
      
      expect(screen.getByText('<html><body>Test</body></html>')).toBeInTheDocument();
    });

    it('displays message when HTML content is not available', () => {
      const snapshotWithoutHtml = { ...mockSnapshot, htmlContent: undefined };
      render(<SnapshotViewer snapshot={snapshotWithoutHtml} viewMode="html" />);
      
      expect(screen.getByText('HTML content not available for this snapshot')).toBeInTheDocument();
    });

    it('does not display screenshot in HTML mode', () => {
      render(<SnapshotViewer snapshot={mockSnapshot} viewMode="html" />);
      
      expect(screen.queryByAltText('Example Domain')).not.toBeInTheDocument();
    });
  });

  describe('Accessibility tree view mode', () => {
    it('displays accessibility tree when viewMode is "tree"', () => {
      render(<SnapshotViewer snapshot={mockSnapshot} viewMode="tree" />);
      
      expect(screen.getByText(/RootWebArea/)).toBeInTheDocument();
      expect(screen.getByText(/button "Click me"/)).toBeInTheDocument();
    });

    it('displays message when accessibility tree is not available', () => {
      const snapshotWithoutTree = { ...mockSnapshot, accessibilityTree: undefined };
      render(<SnapshotViewer snapshot={snapshotWithoutTree} viewMode="tree" />);
      
      expect(screen.getByText('Accessibility tree not available for this snapshot')).toBeInTheDocument();
    });

    it('does not display screenshot in tree mode', () => {
      render(<SnapshotViewer snapshot={mockSnapshot} viewMode="tree" />);
      
      expect(screen.queryByAltText('Example Domain')).not.toBeInTheDocument();
    });
  });

  describe('Edge cases', () => {
    it('handles empty HTML content', () => {
      const snapshotWithEmptyHtml = { ...mockSnapshot, htmlContent: '' };
      render(<SnapshotViewer snapshot={snapshotWithEmptyHtml} viewMode="html" />);
      
      // Empty string should still render (as empty pre tag)
      expect(screen.queryByText('HTML content not available')).not.toBeInTheDocument();
    });

    it('handles empty accessibility tree', () => {
      const snapshotWithEmptyTree = { ...mockSnapshot, accessibilityTree: '' };
      render(<SnapshotViewer snapshot={snapshotWithEmptyTree} viewMode="tree" />);
      
      // Empty string should still render (as empty pre tag)
      expect(screen.queryByText('Accessibility tree not available')).not.toBeInTheDocument();
    });

    it('handles long URLs without breaking layout', () => {
      const snapshotWithLongUrl = {
        ...mockSnapshot,
        url: 'https://example.com/very/long/path/that/might/overflow/the/container/width',
      };
      
      render(<SnapshotViewer snapshot={snapshotWithLongUrl} viewMode="screenshot" />);
      
      const urlElement = screen.getByText(/example.com/);
      expect(urlElement).toHaveClass('truncate');
    });

    it('handles long titles without breaking layout', () => {
      const snapshotWithLongTitle = {
        ...mockSnapshot,
        title: 'This is a very long title that should be truncated to prevent layout issues',
      };
      
      render(<SnapshotViewer snapshot={snapshotWithLongTitle} viewMode="screenshot" />);
      
      const titleElement = screen.getByText(/This is a very long title/);
      expect(titleElement).toHaveClass('truncate');
    });
  });
});
