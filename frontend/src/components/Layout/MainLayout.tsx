import { ReactNode } from 'react';
import { Header } from './Header';
import { useAgentStore } from '../../stores/agentStore';
import { EventStreamPanel } from '../EventStream';
import { BrowserPanel } from '../Browser';
import { FileTreePanel } from '../FileTree';

interface MainLayoutProps {
  conversationListPanel?: ReactNode;
  chatPanel: ReactNode;
  terminalPanel: ReactNode;
  editorPanel: ReactNode;
}

export const MainLayout = ({ conversationListPanel, chatPanel, terminalPanel, editorPanel }: MainLayoutProps) => {
  const { activePanel, currentSessionId } = useAgentStore();

  return (
    <div className="h-screen flex flex-col bg-gray-900 text-white">
      <Header />

      <div className="flex-1 flex overflow-hidden">
        {/* Conversation List Sidebar */}
        {conversationListPanel && (
          <div className="w-64 flex-shrink-0">
            {conversationListPanel}
          </div>
        )}

        {/* Middle Panel - Chat */}
        <div className="flex-1 min-w-0 border-r border-gray-700 flex flex-col">
          {chatPanel}
        </div>

        {/* Right Panel - Tool Panels (Terminal/Editor/Browser) */}
        <div className="w-1/2 flex flex-col">
          {/* Tab Bar */}
          <div className="h-12 bg-gray-800 border-b border-gray-700 flex items-center px-4 space-x-2">
            <PanelTab
              name="terminal"
              label="Terminal"
              icon="💻"
              active={activePanel === 'terminal'}
            />
            <PanelTab
              name="editor"
              label="Code Editor"
              icon="📝"
              active={activePanel === 'editor'}
            />
            <PanelTab
              name="events"
              label="Event Stream"
              icon="📊"
              active={activePanel === 'events'}
            />
            <PanelTab
              name="browser"
              label="Browser"
              icon="🌐"
              active={activePanel === 'browser'}
            />
            <PanelTab
              name="files"
              label="Files"
              icon="📂"
              active={activePanel === 'files'}
            />
          </div>

          {/* Panel Content */}
          <div className="flex-1 overflow-hidden">
            {activePanel === 'terminal' && terminalPanel}
            {activePanel === 'editor' && editorPanel}
            {activePanel === 'events' && currentSessionId && (
              <EventStreamPanel sessionId={currentSessionId} autoRefresh={true} />
            )}
            {activePanel === 'browser' && currentSessionId && (
              <BrowserPanel sessionId={currentSessionId} />
            )}
            {activePanel === 'files' && currentSessionId && (
              <FileTreePanel sessionId={currentSessionId} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

interface PanelTabProps {
  name: string;
  label: string;
  icon: string;
  active: boolean;
}

const PanelTab = ({ name, label, icon, active }: PanelTabProps) => {
  const setActivePanel = useAgentStore((state) => state.setActivePanel);

  return (
    <button
      onClick={() => setActivePanel(name as any)}
      className={`
        px-4 py-2 rounded-t-lg text-sm font-medium transition-colors
        ${
          active
            ? 'bg-gray-900 text-white border-t-2 border-blue-500'
            : 'bg-gray-800 text-gray-400 hover:text-white hover:bg-gray-700'
        }
      `}
    >
      <span className="mr-2">{icon}</span>
      {label}
    </button>
  );
};
