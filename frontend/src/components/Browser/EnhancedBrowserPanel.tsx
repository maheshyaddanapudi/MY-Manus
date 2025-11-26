import React, { useState } from 'react';
import { BrowserPanel } from './BrowserPanel';
import { ConsoleViewer } from './ConsoleViewer';
import { NetworkViewer } from './NetworkViewer';

interface EnhancedBrowserPanelProps {
  sessionId: string;
}

type BrowserTab = 'page' | 'console' | 'network';

export const EnhancedBrowserPanel: React.FC<EnhancedBrowserPanelProps> = ({ sessionId }) => {
  const [activeTab, setActiveTab] = useState<BrowserTab>('page');

  return (
    <div className="h-full flex flex-col bg-gray-900">
      {/* Tab Bar */}
      <div className="border-b border-gray-700 flex items-center px-2 bg-gray-800">
        <TabButton
          active={activeTab === 'page'}
          onClick={() => setActiveTab('page')}
          icon="🌐"
          label="Page"
        />
        <TabButton
          active={activeTab === 'console'}
          onClick={() => setActiveTab('console')}
          icon="📝"
          label="Console"
        />
        <TabButton
          active={activeTab === 'network'}
          onClick={() => setActiveTab('network')}
          icon="🌐"
          label="Network"
        />
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'page' && <BrowserPanel sessionId={sessionId} />}
        {activeTab === 'console' && <ConsoleViewer sessionId={sessionId} />}
        {activeTab === 'network' && <NetworkViewer sessionId={sessionId} />}
      </div>
    </div>
  );
};

interface TabButtonProps {
  active: boolean;
  onClick: () => void;
  icon: string;
  label: string;
}

const TabButton: React.FC<TabButtonProps> = ({ active, onClick, icon, label }) => {
  return (
    <button
      onClick={onClick}
      className={`
        px-4 py-2 text-sm font-medium transition-colors border-b-2
        ${
          active
            ? 'text-white border-blue-500 bg-gray-900'
            : 'text-gray-400 border-transparent hover:text-white hover:bg-gray-750'
        }
      `}
    >
      <span className="mr-2">{icon}</span>
      {label}
    </button>
  );
};
