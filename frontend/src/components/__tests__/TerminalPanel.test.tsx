import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TerminalPanel } from '../Terminal/TerminalPanel';
import { useAgentStore } from '../../stores/agentStore';

// Mock useAgentStore
vi.mock('../../stores/agentStore');

// Mock xterm Terminal
const mockTerminal = {
  open: vi.fn(),
  writeln: vi.fn(),
  write: vi.fn(),
  clear: vi.fn(),
  dispose: vi.fn(),
};

vi.mock('@xterm/xterm', () => ({
  Terminal: class MockTerminal {
    open = mockTerminal.open;
    writeln = mockTerminal.writeln;
    write = mockTerminal.write;
    clear = mockTerminal.clear;
    dispose = mockTerminal.dispose;
  },
}));

describe('TerminalPanel', () => {
  const mockClearTerminal = vi.fn();

  beforeAll(() => {
    // Mock matchMedia for xterm.js
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: vi.fn().mockImplementation(query => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    });
  });

  beforeEach(() => {
    vi.clearAllMocks();
    
    // Default mock for useAgentStore
    (useAgentStore as any).mockReturnValue({
      terminalOutput: [],
      clearTerminal: mockClearTerminal,
    });
  });

  it('renders terminal panel with toolbar', () => {
    render(<TerminalPanel />);
    
    expect(screen.getByText('🐍 Python 3.11 Output')).toBeInTheDocument();
    expect(screen.getByText('Clear')).toBeInTheDocument();
  });

  it('initializes xterm terminal on mount', () => {
    render(<TerminalPanel />);
    
    // Terminal should be opened
    expect(mockTerminal.open).toHaveBeenCalled();
    
    // Welcome messages should be written with ANSI formatting
    expect(mockTerminal.writeln).toHaveBeenCalledWith('\x1b[1;36mMY Manus Terminal\x1b[0m \x1b[32m✓\x1b[0m Ready');
    expect(mockTerminal.writeln).toHaveBeenCalledWith('\x1b[90mWaiting for agent execution...\x1b[0m\n');
  });

  it('displays stdout output from store', () => {
    (useAgentStore as any).mockReturnValue({
      terminalOutput: [
        { type: 'stdout', content: 'Hello, World!' },
      ],
      clearTerminal: mockClearTerminal,
    });

    render(<TerminalPanel />);
    
    // Should write the output to terminal with ANSI formatting
    expect(mockTerminal.write).toHaveBeenCalledWith('\x1b[37mHello, World!\x1b[0m\n');
  });

  it('displays stderr output in red', () => {
    (useAgentStore as any).mockReturnValue({
      terminalOutput: [
        { type: 'stderr', content: 'Error occurred' },
      ],
      clearTerminal: mockClearTerminal,
    });

    render(<TerminalPanel />);
    
    // Should write error in red with ✗ prefix (ANSI escape codes)
    expect(mockTerminal.write).toHaveBeenCalledWith('\x1b[31m✗ Error occurred\x1b[0m\n');
  });

  it('displays multiple outputs in order', () => {
    (useAgentStore as any).mockReturnValue({
      terminalOutput: [
        { type: 'stdout', content: 'Line 1' },
        { type: 'stdout', content: 'Line 2' },
        { type: 'stderr', content: 'Error line' },
      ],
      clearTerminal: mockClearTerminal,
    });

    render(<TerminalPanel />);
    
    // Should write all outputs (only last one is written in useEffect) with ✗ prefix
    expect(mockTerminal.write).toHaveBeenCalledWith('\x1b[31m✗ Error line\x1b[0m\n');
  });

  it('clears terminal when clear button is clicked', async () => {
    const user = userEvent.setup();
    render(<TerminalPanel />);
    
    const clearButton = screen.getByText('Clear');
    await user.click(clearButton);
    
    // Should clear the terminal
    expect(mockTerminal.clear).toHaveBeenCalled();
    expect(mockTerminal.writeln).toHaveBeenCalledWith('\x1b[90mTerminal cleared\x1b[0m\n');
    
    // Should call store's clearTerminal
    expect(mockClearTerminal).toHaveBeenCalled();
  });

  it('disposes terminal on unmount', () => {
    const { unmount } = render(<TerminalPanel />);
    
    unmount();
    
    // Should dispose the terminal
    expect(mockTerminal.dispose).toHaveBeenCalled();
  });

  it('handles empty terminal output', () => {
    (useAgentStore as any).mockReturnValue({
      terminalOutput: [],
      clearTerminal: mockClearTerminal,
    });

    render(<TerminalPanel />);
    
    // Should only write welcome messages, not any output
    expect(mockTerminal.write).not.toHaveBeenCalled();
  });

  it('writes only the last output when terminalOutput changes', () => {
    const { rerender } = render(<TerminalPanel />);
    
    // Update with new output
    (useAgentStore as any).mockReturnValue({
      terminalOutput: [
        { type: 'stdout', content: 'First output' },
        { type: 'stdout', content: 'Second output' },
      ],
      clearTerminal: mockClearTerminal,
    });
    
    rerender(<TerminalPanel />);
    
    // Should write only the last output with ANSI formatting
    expect(mockTerminal.write).toHaveBeenCalledWith('\x1b[37mSecond output\x1b[0m\n');
  });
});
