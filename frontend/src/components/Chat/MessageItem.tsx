import type { Message } from '../../types';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { cn } from '../../theme';
import { CollapsibleCodeBlock } from './CollapsibleCodeBlock';
import { CollapsibleThought } from './CollapsibleThought';
import { CollapsibleObservation } from './CollapsibleObservation';

interface MessageItemProps {
  message: Message;
}

export const MessageItem = ({ message }: MessageItemProps) => {
  const isUser = message.role === 'user';
  const isSystem = message.role === 'system';

  // Parse message content to extract <execute> tags and thoughts
  const parseMessageContent = (content: string) => {
    const parts: Array<{ 
      type: 'text' | 'code' | 'thought'; 
      content: string;
      observation?: string; // Observation/output for code blocks
    }> = [];
    
    // First check if the entire message is a thought (no <execute> tags)
    // Thoughts typically don't have <execute> tags and are explanatory
    const hasExecuteTags = /<execute>/.test(content);
    
    if (!hasExecuteTags && content.trim().length > 50) {
      // Check if this is a final summary/result (don't collapse these)
      const isFinalSummary = /^##?\s*✅|Task completed|successfully created|Summary:/i.test(content);
      
      if (isFinalSummary) {
        // Show final summaries as regular text, not collapsible
        return [{ type: 'text', content }];
      }
      
      // This is likely a thought - make it collapsible
      return [{ type: 'thought', content }];
    }
    
    // Parse <execute> tags with optional observation
    const regex = /<execute>([\s\S]*?)<\/execute>(?:\s*<observation>([\s\S]*?)<\/observation>)?/g;
    let lastIndex = 0;
    let match;

    while ((match = regex.exec(content)) !== null) {
      // Add text before the execute tag
      if (match.index > lastIndex) {
        const textContent = content.substring(lastIndex, match.index).trim();
        if (textContent) {
          parts.push({
            type: 'text',
            content: textContent
          });
        }
      }
      
      // Add the code from execute tag with optional observation
      parts.push({
        type: 'code',
        content: match[1].trim(),
        observation: match[2] ? match[2].trim() : undefined
      });
      
      lastIndex = regex.lastIndex;
    }
    
    // Add remaining text after last execute tag
    if (lastIndex < content.length) {
      const remaining = content.substring(lastIndex).trim();
      if (remaining) {
        parts.push({
          type: 'text',
          content: remaining
        });
      }
    }
    
    return parts.length > 0 ? parts : [{ type: 'text', content }];
  };

  const contentParts = !isUser ? parseMessageContent(message.content) : [];

  return (
    <div
      className={cn(
        'flex flex-col mb-6',
        isUser ? 'items-end' : 'items-start'
      )}
    >
      {/* Message Bubble - Only contains text content */}
      {(isUser || contentParts.some(p => p.type === 'text')) && (
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

          {/* Content - Only text parts */}
          <div className={cn(
            'prose prose-invert max-w-none',
            isUser ? 'prose-p:text-white' : 'prose-p:text-gray-200'
          )}>
            {isUser ? (
              <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
            ) : (
              <div>
                {contentParts
                  .filter(part => part.type === 'text')
                  .map((part, index) => (
                    <ReactMarkdown
                      key={index}
                      components={{
                        code(props: any) {
                          const { node, inline, className, children, ...rest } = props;
                          const match = /language-(\w+)/.exec(className || '');
                          
                          if (!inline && match) {
                            const code = String(children).replace(/\n$/, '');
                            const language = match[1];
                            
                            return (
                              <div className="my-3 rounded-lg overflow-hidden border border-gray-700/50">
                                <SyntaxHighlighter
                                  style={vscDarkPlus as any}
                                  language={language}
                                  PreTag="div"
                                  customStyle={{
                                    margin: 0,
                                    borderRadius: '0.5rem',
                                    fontSize: '0.875rem',
                                  }}
                                >
                                  {code}
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
                        h1(props) {
                          return <h1 className="text-2xl font-bold mb-3 mt-4" {...props} />;
                        },
                        h2(props) {
                          return <h2 className="text-xl font-bold mb-2 mt-3" {...props} />;
                        },
                        h3(props) {
                          return <h3 className="text-lg font-bold mb-2 mt-2" {...props} />;
                        },
                      }}
                    >
                      {part.content}
                    </ReactMarkdown>
                  ))}
              </div>
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
      )}

      {/* Collapsible Content - Rendered outside bubble but aligned */}
      {!isUser && contentParts.some(p => p.type === 'code' || p.type === 'thought') && (
        <div className="w-[75%] mt-2 space-y-2">
          {(contentParts
            .filter(part => part.type === 'code' || part.type === 'thought') as Array<{ type: 'text' | 'code' | 'thought'; content: string; observation?: string }>)
            .map((part, index) => (
              <div key={index}>
                {part.type === 'code' ? (
                  <>
                    <CollapsibleCodeBlock
                      code={part.content}
                      language="python"
                    />
                    {/* Show observation inline after code block */}
                    {part.observation && (
                      <CollapsibleObservation content={part.observation} />
                    )}
                  </>
                ) : (
                  <CollapsibleThought
                    content={part.content}
                  />
                )}
              </div>
            ))}
        </div>
      )}
    </div>
  );
};
