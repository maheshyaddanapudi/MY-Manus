import { useState, useRef, useEffect } from 'react';
import { useAgentStore } from '../../stores/agentStore';
import { MessageList } from './MessageList';
import { ChatInput } from './ChatInput';
import { apiService } from '../../services/api';

export const ChatPanel = () => {
  const { sessionId, messages, addMessage, setAgentStatus } = useAgentStore();
  const [isProcessing, setIsProcessing] = useState(false);

  const handleSendMessage = async (content: string) => {
    if (!content.trim() || isProcessing) return;

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
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Messages */}
      <div className="flex-1 overflow-hidden">
        <MessageList messages={messages} />
      </div>

      {/* Input */}
      <div className="border-t border-gray-700">
        <ChatInput
          onSend={handleSendMessage}
          disabled={isProcessing}
          placeholder={
            isProcessing
              ? 'Agent is processing...'
              : 'Ask the agent to solve a task...'
          }
        />
      </div>
    </div>
  );
};
