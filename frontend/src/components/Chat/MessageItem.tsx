import type { Message } from '../../types';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { cn } from '../../theme';

interface MessageItemProps {
  message: Message;
}

export const MessageItem = ({ message }: MessageItemProps) => {
  const isUser = message.role === 'user';
  const isSystem = message.role === 'system';

  return (
    <div
      className={cn(
        'flex mb-6',
        isUser ? 'justify-end' : 'justify-start'
      )}
    >
      <div
        className={cn(
          'max-w-[75%] rounded-2xl px-5 py-4 shadow-lg transition-all duration-200 hover:shadow-xl',
          isUser
            ? 'bg-gradient-to-br from-blue-600 to-blue-700 text-white rounded-tr-sm'
            : isSystem
            ? 'bg-gradient-to-br from-yellow-900/40 to-yellow-800/40 text-yellow-100 border border-yellow-700/50 rounded-tl-sm'
            : 'bg-gradient-to-br from-gray-800 to-gray-800/95 text-gray-100 border border-gray-700/50 rounded-tl-sm'
        )}
      >
        {/* Role indicator with avatar */}
        <div className="flex items-center gap-2 mb-3">
          <div
            className={cn(
              'w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold',
              isUser
                ? 'bg-white/20'
                : isSystem
                ? 'bg-yellow-500/20'
                : 'bg-gradient-to-br from-purple-500 to-blue-500'
            )}
          >
            {isUser ? '👤' : isSystem ? '⚠️' : '🤖'}
          </div>
          <span className="text-xs font-semibold opacity-90">
            {isUser ? 'You' : isSystem ? 'System' : 'MY Manus'}
          </span>
        </div>

        {/* Content */}
        <div className={cn(
          'prose prose-invert max-w-none',
          isUser ? 'prose-p:text-white' : 'prose-p:text-gray-200'
        )}>
          {isUser ? (
            <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
          ) : (
            <ReactMarkdown
              components={{
                code(props: any) {
                  const { node, inline, className, children, ...rest } = props;
                  const match = /language-(\w+)/.exec(className || '');
                  
                  if (!inline && match) {
                    return (
                      <div className="my-3 rounded-lg overflow-hidden border border-gray-700/50">
                        <SyntaxHighlighter
                          style={vscDarkPlus as any}
                          language={match[1]}
                          PreTag="div"
                          customStyle={{
                            margin: 0,
                            borderRadius: '0.5rem',
                            fontSize: '0.875rem',
                          }}
                        >
                          {String(children).replace(/\n$/, '')}
                        </SyntaxHighlighter>
                      </div>
                    );
                  }
                  
                  return (
                    <code
                      className={cn(
                        className,
                        'px-1.5 py-0.5 rounded bg-gray-900/50 text-blue-300 text-sm font-mono'
                      )}
                      {...rest}
                    >
                      {children}
                    </code>
                  );
                },
                p(props) {
                  return <p className="leading-relaxed mb-3 last:mb-0" {...props} />;
                },
                ul(props) {
                  return <ul className="list-disc list-inside space-y-1 my-2" {...props} />;
                },
                ol(props) {
                  return <ol className="list-decimal list-inside space-y-1 my-2" {...props} />;
                },
              }}
            >
              {message.content}
            </ReactMarkdown>
          )}
        </div>

        {/* Timestamp */}
        <div className={cn(
          'text-xs mt-3 pt-2 border-t',
          isUser
            ? 'opacity-60 border-white/20'
            : 'opacity-50 border-gray-700/50'
        )}>
          {new Date(message.timestamp).toLocaleTimeString([], {
            hour: '2-digit',
            minute: '2-digit'
          })}
        </div>
      </div>
    </div>
  );
};
