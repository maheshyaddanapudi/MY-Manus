import { useAgentStore } from '../../stores/agentStore';

export const Header = () => {
  const { sessionId, isConnected, agentStatus } = useAgentStore();

  const getStatusColor = () => {
    if (!isConnected) return 'bg-gray-500';
    switch (agentStatus) {
      case 'thinking':
        return 'bg-blue-500 animate-pulse';
      case 'executing':
        return 'bg-yellow-500 animate-pulse';
      case 'error':
        return 'bg-red-500';
      case 'done':
        return 'bg-green-500';
      default:
        return 'bg-gray-400';
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
        {/* Session Info */}
        {sessionId && (
          <div className="text-sm text-gray-400">
            <span className="font-mono">Session: {sessionId.substring(0, 8)}...</span>
          </div>
        )}

        {/* Status Indicator */}
        <div className="flex items-center space-x-2">
          <div className={`w-3 h-3 rounded-full ${getStatusColor()}`}></div>
          <span className="text-sm text-gray-300">{getStatusText()}</span>
        </div>
      </div>
    </header>
  );
};
