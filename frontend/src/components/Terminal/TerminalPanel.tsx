import { useEffect, useRef } from 'react';
import { Terminal } from '@xterm/xterm';
import '@xterm/xterm/css/xterm.css';
import { useAgentStore } from '../../stores/agentStore';

export const TerminalPanel = () => {
  const terminalRef = useRef<HTMLDivElement>(null);
  const xtermRef = useRef<Terminal | null>(null);
  const { terminalOutput, clearTerminal } = useAgentStore();

  useEffect(() => {
    if (!terminalRef.current) return;

    // Initialize xterm.js
    const terminal = new Terminal({
      theme: {
        background: '#1e1e1e',
        foreground: '#d4d4d4',
        cursor: '#d4d4d4',
        black: '#000000',
        red: '#cd3131',
        green: '#0dbc79',
        yellow: '#e5e510',
        blue: '#2472c8',
        magenta: '#bc3fbc',
        cyan: '#11a8cd',
        white: '#e5e5e5',
        brightBlack: '#666666',
        brightRed: '#f14c4c',
        brightGreen: '#23d18b',
        brightYellow: '#f5f543',
        brightBlue: '#3b8eea',
        brightMagenta: '#d670d6',
        brightCyan: '#29b8db',
        brightWhite: '#e5e5e5',
      },
      fontFamily: '"Fira Code", "Courier New", monospace',
      fontSize: 14,
      cursorBlink: false,
      rows: 24,
      convertEol: true,
    });

    terminal.open(terminalRef.current);
    terminal.writeln('MY Manus Terminal - Ready');
    terminal.writeln('Waiting for agent execution...\n');

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
      // Red text for errors
      terminal.write(`\x1b[31m${lastOutput.content}\x1b[0m\n`);
    } else {
      // Normal output
      terminal.write(lastOutput.content + '\n');
    }
  }, [terminalOutput]);

  const handleClear = () => {
    if (xtermRef.current) {
      xtermRef.current.clear();
      xtermRef.current.writeln('Terminal cleared\n');
    }
    clearTerminal();
  };

  return (
    <div className="h-full flex flex-col bg-[#1e1e1e]">
      {/* Terminal toolbar */}
      <div className="h-10 bg-gray-800 border-b border-gray-700 flex items-center justify-between px-4">
        <span className="text-sm text-gray-400 font-mono">Python 3.11 Output</span>
        <button
          onClick={handleClear}
          className="text-xs px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-gray-300 transition-colors"
        >
          Clear
        </button>
      </div>

      {/* Terminal display */}
      <div ref={terminalRef} className="flex-1 p-2" />
    </div>
  );
};
