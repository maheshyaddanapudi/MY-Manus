import { useState } from 'react';
import { useAgentStore } from '../../stores/agentStore';
import { MessageList } from './MessageList';
import { ChatInput } from './ChatInput';
import { apiService } from '../../services/api';

export const ChatPanel = () => {
  const { sessionId, messages, addMessage, setAgentStatus } = useAgentStore();
  const [isProcessing, setIsProcessing] = useState(false);

  const handleSendMessage = async (content: string) => {
    if (!content.trim()) return;

    // Add user message to UI
    const userMessage = {
      id: `user-${Date.now()}`,
      role: 'user' as const,
      content,
      timestamp: new Date(),
    };
    addMessage(userMessage);

    setIsProcessing(true);
    setAgentStatus('thinking');

    try {
      // Send to backend
      const response = await apiService.chat({
        sessionId: sessionId || undefined,
        message: content,
      });

      // Note: Real response will come via WebSocket events
      // This is just the final completion message
      console.log('Chat response:', response);
    } catch (error) {
      console.error('Error sending message:', error);
      setAgentStatus('error');

      // Add error message
      addMessage({
        id: `error-${Date.now()}`,
        role: 'system',
        content: `Error: ${error instanceof Error ? error.message : 'Unknown error'}`,
        timestamp: new Date(),
      });
    } finally {
      setIsProcessing(false);
      setAgentStatus('idle');
    }
  };

  const handleStopAgent = async () => {
    if (!sessionId) {
      console.warn('No session ID to stop');
      return;
    }

    try {
      console.log('Stopping agent for session:', sessionId);
      const response = await apiService.stopAgent(sessionId);
      console.log('Stop response:', response);

      // Add system message about stopping
      addMessage({
        id: `system-${Date.now()}`,
        role: 'system',
        content: 'Stop requested. Agent will stop at the next iteration.',
        timestamp: new Date(),
      });
    } catch (error) {
      console.error('Error stopping agent:', error);
      // Still try to update local state
    }
  };

  return (
    <div className="h-full flex flex-col bg-gradient-to-b from-gray-900 to-gray-900/95">
      {/* Messages */}
      <div className="flex-1 overflow-hidden">
        <MessageList messages={messages} />
      </div>

      {/* Input */}
      <ChatInput
        onSend={handleSendMessage}
        onStop={handleStopAgent}
        isAgentRunning={isProcessing}
      />
    </div>
  );
};
