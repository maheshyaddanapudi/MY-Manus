import React from 'react';
import { useAgentStore } from '../../stores/agentStore';
import { cn } from '../../theme';

const PANEL_NAMES: Record<string, string> = {
  terminal: 'Terminal',
  editor: 'Editor',
  browser: 'Browser',
  events: 'Event Stream',
  files: 'Files',
  replay: 'Replay',
  knowledge: 'Knowledge',
  plan: 'Plan',
};

const PANEL_ICONS: Record<string, string> = {
  terminal: '💻',
  editor: '📝',
  browser: '🌐',
  events: '📊',
  files: '📂',
  replay: '⏮️',
  knowledge: '📚',
  plan: '📋',
};

export const ActivityBanner: React.FC = () => {
  const { activePanel, suggestedPanel, setActivePanel } = useAgentStore();

  // Don't show banner if no suggestion or already on suggested panel
  if (!suggestedPanel || activePanel === suggestedPanel) {
    return null;
  }

  const handleGoToPanel = () => {
    setActivePanel(suggestedPanel);
    // Clear suggestion after navigating
    useAgentStore.setState({ suggestedPanel: null });
  };

  return (
    <div className={cn(
      'fixed bottom-4 left-1/2 transform -translate-x-1/2',
      'bg-gradient-to-r from-blue-600 to-blue-700',
      'text-white px-6 py-3 rounded-full shadow-2xl',
      'flex items-center gap-3',
      'z-50 border border-blue-400/30'
    )}>
      <span className="text-lg">{PANEL_ICONS[suggestedPanel]}</span>
      <span className="font-medium">
        <span className="font-bold">Currently Active:</span> {PANEL_NAMES[suggestedPanel]}
      </span>
      <button
        onClick={handleGoToPanel}
        className={cn(
          'ml-2 px-4 py-1.5 rounded-full',
          'bg-white/20 hover:bg-white/30',
          'transition-colors font-medium text-sm',
          'border border-white/30'
        )}
      >
        Go there →
      </button>
      <button
        onClick={() => useAgentStore.setState({ suggestedPanel: null })}
        className={cn(
          'ml-1 w-6 h-6 rounded-full',
          'hover:bg-white/20 transition-colors',
          'flex items-center justify-center text-xs'
        )}
      >
        ✕
      </button>
    </div>
  );
};
