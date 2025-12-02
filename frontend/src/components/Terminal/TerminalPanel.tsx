import { useEffect, useRef } from 'react';
import { Terminal } from '@xterm/xterm';
import '@xterm/xterm/css/xterm.css';
import { useAgentStore } from '../../stores/agentStore';
import { cn, getButtonClasses } from '../../theme';

export const TerminalPanel = () => {
  const terminalRef = useRef<HTMLDivElement>(null);
  const xtermRef = useRef<Terminal | null>(null);
  const { terminalOutput, clearTerminal } = useAgentStore();

  useEffect(() => {
    if (!terminalRef.current) return;

    // Initialize xterm.js with themed colors
    const terminal = new Terminal({
      theme: {
        background: '#0f172a', // Match our dark theme
        foreground: '#e2e8f0',
        cursor: '#60a5fa',
        cursorAccent: '#1e293b',
        selectionBackground: '#3b82f680',
        black: '#1e293b',
        red: '#ef4444',
        green: '#22c55e',
        yellow: '#f59e0b',
        blue: '#3b82f6',
        magenta: '#a855f7',
        cyan: '#06b6d4',
        white: '#e2e8f0',
        brightBlack: '#475569',
        brightRed: '#f87171',
        brightGreen: '#4ade80',
        brightYellow: '#fbbf24',
        brightBlue: '#60a5fa',
        brightMagenta: '#c084fc',
        brightCyan: '#22d3ee',
        brightWhite: '#f8fafc',
      },
      fontFamily: '"SF Mono", "Monaco", "Inconsolata", "Fira Code", "Courier New", monospace',
      fontSize: 13,
      fontWeight: '400',
      fontWeightBold: '600',
      lineHeight: 1.4,
      cursorBlink: true,
      cursorStyle: 'block',
      rows: 24,
      convertEol: true,
      scrollback: 1000,
    });

    terminal.open(terminalRef.current);
    terminal.writeln('\x1b[1;36mMY Manus Terminal\x1b[0m \x1b[32m✓\x1b[0m Ready');
    terminal.writeln('\x1b[90mWaiting for agent execution...\x1b[0m\n');

    xtermRef.current = terminal;

    // Cleanup
    return () => {
      terminal.dispose();
    };
  }, []);

  // Write terminal output
  useEffect(() => {
    if (!xtermRef.current || terminalOutput.length === 0) return;

    const lastOutput = terminalOutput[terminalOutput.length - 1];
    const terminal = xtermRef.current;

    if (lastOutput.type === 'stderr') {
      // Red text for errors with icon
      terminal.write(`\x1b[31m✗ ${lastOutput.content}\x1b[0m\n`);
    } else {
      // Normal output with subtle styling
      terminal.write(`\x1b[37m${lastOutput.content}\x1b[0m\n`);
    }
  }, [terminalOutput]);

  const handleClear = () => {
    if (xtermRef.current) {
      xtermRef.current.clear();
      xtermRef.current.writeln('\x1b[90mTerminal cleared\x1b[0m\n');
    }
    clearTerminal();
  };

  return (
    <div className="h-full flex flex-col bg-gradient-to-b from-gray-900 to-gray-900/95">
      {/* Terminal toolbar */}
      <div className="h-12 bg-gray-800/60 border-b border-gray-700/50 flex items-center justify-between px-4 backdrop-blur-sm">
        <div className="flex items-center gap-3">
          <div className="flex gap-1.5">
            <div className="w-3 h-3 rounded-full bg-red-500/80"></div>
            <div className="w-3 h-3 rounded-full bg-yellow-500/80"></div>
            <div className="w-3 h-3 rounded-full bg-green-500/80"></div>
          </div>
          <span className="text-sm text-gray-400 font-mono font-medium">
            🐍 Python 3.11 Output
          </span>
        </div>
        <button
          onClick={handleClear}
          className={cn(
            getButtonClasses('ghost', 'sm'),
            'text-xs'
          )}
        >
          Clear
        </button>
      </div>

      {/* Terminal display */}
      <div ref={terminalRef} className="flex-1 p-3" />
    </div>
  );
};
