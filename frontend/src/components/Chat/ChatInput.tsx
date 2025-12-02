import { useState } from 'react';
import type { KeyboardEvent } from 'react';
import { PaperAirplaneIcon } from '@heroicons/react/24/solid';
import { cn, getButtonClasses } from '../../theme';

interface ChatInputProps {
  onSend: (message: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

export const ChatInput = ({ onSend, disabled, placeholder }: ChatInputProps) => {
  const [message, setMessage] = useState('');
  const [isFocused, setIsFocused] = useState(false);

  const handleSend = () => {
    if (message.trim() && !disabled) {
      onSend(message);
      setMessage('');
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="p-4 bg-gradient-to-t from-gray-900 to-gray-800/95 border-t border-gray-700/50 backdrop-blur-sm">
      <div className={cn(
        'flex gap-3 p-3 rounded-xl transition-all duration-200',
        'bg-gray-800/60 border',
        isFocused
          ? 'border-blue-500/50 shadow-lg shadow-blue-500/10'
          : 'border-gray-700/50'
      )}>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          disabled={disabled}
          placeholder={placeholder || 'Ask the agent to solve a task...'}
          className={cn(
            'flex-1 bg-transparent text-gray-100 placeholder-gray-500',
            'focus:outline-none resize-none',
            'disabled:opacity-50 disabled:cursor-not-allowed',
            'leading-relaxed'
          )}
          rows={3}
          style={{ minHeight: '60px', maxHeight: '200px' }}
        />
        <button
          onClick={handleSend}
          disabled={disabled || !message.trim()}
          className={cn(
            getButtonClasses('primary', 'md'),
            'self-end px-5 shadow-lg hover:shadow-xl',
            'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:shadow-lg',
            'transition-all duration-200'
          )}
          title="Send message (Enter)"
        >
          <PaperAirplaneIcon className="w-5 h-5" />
          <span className="font-semibold">Send</span>
        </button>
      </div>
      <div className="mt-2 px-1 text-xs text-gray-500 flex items-center gap-2">
        <span>💡</span>
        <span>Press <kbd className="px-1.5 py-0.5 bg-gray-800 border border-gray-700 rounded text-gray-400 font-mono text-xs">Enter</kbd> to send, <kbd className="px-1.5 py-0.5 bg-gray-800 border border-gray-700 rounded text-gray-400 font-mono text-xs">Shift+Enter</kbd> for new line</span>
      </div>
    </div>
  );
};
