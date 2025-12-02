import { useAgentStore } from '../../stores/agentStore';
import { NotificationBell } from '../Notifications';
import { theme } from '../../theme';

export const Header = () => {
  const { isConnected, agentStatus } = useAgentStore();

  const getStatusStyle = (): React.CSSProperties => {
    const baseStyle: React.CSSProperties = {
      width: '10px',
      height: '10px',
      borderRadius: '50%',
      flexShrink: 0,
    };

    if (!isConnected) {
      return {
        ...baseStyle,
        backgroundColor: theme.colors.status.offline,
      };
    }

    switch (agentStatus) {
      case 'thinking':
      case 'executing':
        return {
          ...baseStyle,
          backgroundColor: theme.colors.status.online,
          animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        };
      case 'error':
        return {
          ...baseStyle,
          backgroundColor: theme.colors.status.offline,
        };
      case 'done':
      case 'idle':
      default:
        return {
          ...baseStyle,
          backgroundColor: theme.colors.status.online,
        };
    }
  };

  const getStatusText = () => {
    if (!isConnected) return 'Disconnected';
    return agentStatus.charAt(0).toUpperCase() + agentStatus.slice(1);
  };

  const getStatusColor = () => {
    if (!isConnected) return 'text-red-400';
    switch (agentStatus) {
      case 'error':
        return 'text-red-400';
      case 'thinking':
      case 'executing':
        return 'text-green-400';
      default:
        return 'text-gray-300';
    }
  };

  return (
    <header className="h-16 bg-gradient-to-r from-gray-900 via-gray-900 to-gray-800 border-b border-gray-700/50 backdrop-blur-sm flex items-center justify-between px-6 shadow-sm">
      {/* Left: Logo and Title */}
      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-3">
          {/* Logo */}
          <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center shadow-lg">
            <span className="text-white font-bold text-sm">M</span>
          </div>
          
          {/* Title */}
          <div className="flex flex-col">
            <h1 className="text-lg font-bold text-white tracking-tight">MY Manus</h1>
            <span className="text-xs text-gray-400 font-medium">CodeAct AI Agent</span>
          </div>
        </div>
      </div>

      {/* Right: Status and Notifications */}
      <div className="flex items-center space-x-4">
        {/* Connection Status Indicator */}
        <div className="flex items-center space-x-2.5 px-4 py-2 rounded-lg bg-gray-800/60 border border-gray-700/50 backdrop-blur-sm shadow-sm hover:bg-gray-800/80 transition-all duration-200">
          <div style={getStatusStyle()}></div>
          <span className={`text-sm font-medium ${getStatusColor()} transition-colors duration-200`}>
            {getStatusText()}
          </span>
        </div>

        {/* Notification Bell */}
        <NotificationBell />
      </div>
    </header>
  );
};
