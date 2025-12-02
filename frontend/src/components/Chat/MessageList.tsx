import { useEffect, useRef } from 'react';
import type { Message } from '../../types';
import { MessageItem } from './MessageItem';

interface MessageListProps {
  messages: Message[];
}

export const MessageList = ({ messages }: MessageListProps) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  if (messages.length === 0) {
    return (
      <div className="h-full flex items-center justify-center p-8">
        <div className="text-center text-gray-400 max-w-lg">
          <div className="relative inline-block mb-6">
            <div className="text-7xl animate-pulse">🤖</div>
            <div className="absolute -bottom-2 -right-2 w-6 h-6 bg-gradient-to-br from-green-500 to-green-600 rounded-full border-4 border-gray-900"></div>
          </div>
          <h2 className="text-3xl font-bold mb-3 bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">
            Welcome to MY Manus
          </h2>
          <p className="text-base text-gray-400 leading-relaxed mb-6">
            I'm an AI agent that solves problems by writing and executing Python code.
          </p>
          <div className="grid grid-cols-1 gap-3 text-sm text-left">
            <div className="flex items-start gap-3 p-3 rounded-lg bg-gray-800/40 border border-gray-700/50">
              <span className="text-xl">📊</span>
              <div>
                <div className="font-medium text-gray-300">Data Analysis</div>
                <div className="text-gray-500 text-xs">Analyze datasets and create visualizations</div>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 rounded-lg bg-gray-800/40 border border-gray-700/50">
              <span className="text-xl">🔍</span>
              <div>
                <div className="font-medium text-gray-300">Web Search</div>
                <div className="text-gray-500 text-xs">Search the web and gather information</div>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 rounded-lg bg-gray-800/40 border border-gray-700/50">
              <span className="text-xl">⚡</span>
              <div>
                <div className="font-medium text-gray-300">Task Automation</div>
                <div className="text-gray-500 text-xs">Solve complex tasks with code execution</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full overflow-y-auto p-6 custom-scrollbar">
      <div className="max-w-4xl mx-auto">
        {messages.map((message) => (
          <MessageItem key={message.id} message={message} />
        ))}
        <div ref={messagesEndRef} />
      </div>
    </div>
  );
};
