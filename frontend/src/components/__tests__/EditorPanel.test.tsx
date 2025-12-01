import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EditorPanel } from '../Editor/EditorPanel';
import { useAgentStore } from '../../stores/agentStore';

// Mock useAgentStore
vi.mock('../../stores/agentStore');

// Mock Monaco Editor
vi.mock('@monaco-editor/react', () => ({
  default: ({ value, defaultLanguage, theme }: any) => (
    <div data-testid="monaco-editor" data-language={defaultLanguage} data-theme={theme}>
      {value}
    </div>
  ),
}));

describe('EditorPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders editor panel with toolbar', () => {
    (useAgentStore as any).mockReturnValue({
      currentCode: '',
      codeHistory: [],
    });

    render(<EditorPanel />);
    
    expect(screen.getByText('Current Code')).toBeInTheDocument();
  });

  it('displays current code in Monaco Editor', () => {
    const pythonCode = "print('Hello, World!')";
    
    (useAgentStore as any).mockReturnValue({
      currentCode: pythonCode,
      codeHistory: [],
    });

    render(<EditorPanel />);
    
    const editor = screen.getByTestId('monaco-editor');
    expect(editor).toBeInTheDocument();
    expect(editor).toHaveTextContent(pythonCode);
    expect(editor).toHaveAttribute('data-language', 'python');
    expect(editor).toHaveAttribute('data-theme', 'vs-dark');
  });

  it('displays Python code from agent', () => {
    const pythonCode = `def hello():
    return "world"`;
    
    (useAgentStore as any).mockReturnValue({
      currentCode: pythonCode,
      codeHistory: [],
    });

    render(<EditorPanel />);
    
    expect(screen.getByText(/def hello/)).toBeInTheDocument();
    expect(screen.getByText(/return "world"/)).toBeInTheDocument();
  });

  it('shows empty state when no code is available', () => {
    (useAgentStore as any).mockReturnValue({
      currentCode: '',
      codeHistory: [],
    });

    render(<EditorPanel />);
    
    expect(screen.getByText('Waiting for agent to generate code...')).toBeInTheDocument();
    expect(screen.queryByTestId('monaco-editor')).not.toBeInTheDocument();
  });

  it('displays code history count', () => {
    (useAgentStore as any).mockReturnValue({
      currentCode: 'print("current")',
      codeHistory: [
        { code: 'print("v1")', iteration: 1 },
        { code: 'print("v2")', iteration: 2 },
        { code: 'print("v3")', iteration: 3 },
      ],
    });

    render(<EditorPanel />);
    
    expect(screen.getByText('(3 executions)')).toBeInTheDocument();
  });

  it('shows code history dropdown when history exists', () => {
    (useAgentStore as any).mockReturnValue({
      currentCode: 'print("current")',
      codeHistory: [
        { code: 'print("v1")', iteration: 1 },
        { code: 'print("v2")', iteration: 2 },
      ],
    });

    render(<EditorPanel />);
    
    const dropdown = screen.getByRole('combobox');
    expect(dropdown).toBeInTheDocument();
    
    // Should have options for current + history
    expect(screen.getByText('Current')).toBeInTheDocument();
    expect(screen.getByText('Execution 1 (Iter 1)')).toBeInTheDocument();
    expect(screen.getByText('Execution 2 (Iter 2)')).toBeInTheDocument();
  });

  it('does not show dropdown when no history', () => {
    (useAgentStore as any).mockReturnValue({
      currentCode: 'print("current")',
      codeHistory: [],
    });

    render(<EditorPanel />);
    
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('switches to historical code when dropdown option is selected', async () => {
    const user = userEvent.setup();
    
    (useAgentStore as any).mockReturnValue({
      currentCode: 'print("current")',
      codeHistory: [
        { code: 'print("v1")', iteration: 1 },
        { code: 'print("v2")', iteration: 2 },
      ],
    });

    render(<EditorPanel />);
    
    // Initially shows current code
    expect(screen.getByText('print("current")')).toBeInTheDocument();
    
    // Select first execution from history
    const dropdown = screen.getByRole('combobox');
    await user.selectOptions(dropdown, '0');
    
    // Should now show historical code
    expect(screen.getByText('print("v1")')).toBeInTheDocument();
    expect(screen.getByText('Iteration 1')).toBeInTheDocument();
  });

  it('switches back to current code when "Current" is selected', async () => {
    const user = userEvent.setup();
    
    (useAgentStore as any).mockReturnValue({
      currentCode: 'print("current")',
      codeHistory: [
        { code: 'print("v1")', iteration: 1 },
      ],
    });

    render(<EditorPanel />);
    
    const dropdown = screen.getByRole('combobox');
    
    // Select historical code
    await user.selectOptions(dropdown, '0');
    expect(screen.getByText('print("v1")')).toBeInTheDocument();
    
    // Switch back to current
    await user.selectOptions(dropdown, '-1');
    expect(screen.getByText('print("current")')).toBeInTheDocument();
    expect(screen.getByText('Current Code')).toBeInTheDocument();
  });

  it('displays iteration number for historical code', async () => {
    const user = userEvent.setup();
    
    (useAgentStore as any).mockReturnValue({
      currentCode: 'print("current")',
      codeHistory: [
        { code: 'print("v1")', iteration: 5 },
      ],
    });

    render(<EditorPanel />);
    
    const dropdown = screen.getByRole('combobox');
    await user.selectOptions(dropdown, '0');
    
    expect(screen.getByText('Iteration 5')).toBeInTheDocument();
  });

  it('handles empty current code with history', () => {
    (useAgentStore as any).mockReturnValue({
      currentCode: '',
      codeHistory: [
        { code: 'print("v1")', iteration: 1 },
      ],
    });

    render(<EditorPanel />);
    
    // Should show empty state for current code
    expect(screen.getByText('Waiting for agent to generate code...')).toBeInTheDocument();
    
    // But dropdown should still be available
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });

  it('auto-resets to current code when new code arrives', () => {
    const { rerender } = render(<EditorPanel />);
    
    // Start with some history selected
    (useAgentStore as any).mockReturnValue({
      currentCode: 'print("v1")',
      codeHistory: [
        { code: 'print("old")', iteration: 1 },
      ],
    });
    
    rerender(<EditorPanel />);
    
    // User selects historical code (this would happen via user interaction)
    // But when new code arrives, it should auto-reset to show current
    
    (useAgentStore as any).mockReturnValue({
      currentCode: 'print("v2")', // New code
      codeHistory: [
        { code: 'print("old")', iteration: 1 },
        { code: 'print("v1")', iteration: 2 },
      ],
    });
    
    rerender(<EditorPanel />);
    
    // Should show current code
    expect(screen.getByText('print("v2")')).toBeInTheDocument();
  });
});
