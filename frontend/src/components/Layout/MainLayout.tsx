import { ReactNode } from 'react';
import { Header } from './Header';
import { useAgentStore } from '../../stores/agentStore';

interface MainLayoutProps {
  chatPanel: ReactNode;
  terminalPanel: ReactNode;
  editorPanel: ReactNode;
}

export const MainLayout = ({ chatPanel, terminalPanel, editorPanel }: MainLayoutProps) => {
  const { activePanel } = useAgentStore();

  return (
    <div className="h-screen flex flex-col bg-gray-900 text-white">
      <Header />

      <div className="flex-1 flex overflow-hidden">
        {/* Left Panel - Chat */}
        <div className="w-1/2 border-r border-gray-700 flex flex-col">
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
              name="browser"
              label="Browser"
              icon="🌐"
              active={activePanel === 'browser'}
            />
          </div>

          {/* Panel Content */}
          <div className="flex-1 overflow-hidden">
            {activePanel === 'terminal' && terminalPanel}
            {activePanel === 'editor' && editorPanel}
            {activePanel === 'browser' && (
              <div className="h-full flex items-center justify-center text-gray-500">
                Browser panel (coming soon)
              </div>
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
