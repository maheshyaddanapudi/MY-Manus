import { useAgentStore } from '../../stores/agentStore';
import { NotificationBell } from '../Notifications';

export const Header = () => {
  const { isConnected, agentStatus } = useAgentStore();

  const getStatusColor = () => {
    if (!isConnected) return 'bg-red-500';
    switch (agentStatus) {
      case 'thinking':
        return 'bg-green-500 animate-pulse';
      case 'executing':
        return 'bg-green-500 animate-pulse';
      case 'error':
        return 'bg-red-500';
      case 'done':
        return 'bg-green-500';
      default:
        return 'bg-green-500'; // idle state - connected
    }
  };

  const getStatusText = () => {
    if (!isConnected) return 'Disconnected';
    return agentStatus.charAt(0).toUpperCase() + agentStatus.slice(1);
  };

  return (
    <header className="h-16 bg-gray-900 border-b border-gray-700 flex items-center justify-between px-6">
      <div className="flex items-center space-x-4">
        <h1 className="text-xl font-bold text-white">MY Manus</h1>
        <span className="text-sm text-gray-400">CodeAct AI Agent</span>
      </div>

      <div className="flex items-center space-x-6">
        {/* Connection Status Indicator */}
        <div className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-gray-800/50">
          <div className={`w-2.5 h-2.5 rounded-full ${getStatusColor()}`}></div>
          <span className="text-sm font-medium text-gray-200">{getStatusText()}</span>
        </div>

        {/* Notification Bell */}
        <NotificationBell />
      </div>
    </header>
  );
};
