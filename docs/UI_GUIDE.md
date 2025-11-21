# UI Implementation Guide

## Three-Panel Layout

Mirror Manus AI's transparent interface showing agent work in real-time.

### Panel Structure
1. **Chat Panel** (Left): Conversation with agent
2. **Terminal Panel** (Top Right): Command execution
3. **Editor Panel** (Bottom Right): Code editing
4. **Browser Panel** (Tab): Web browsing when needed

## Component Architecture

### Core Components
```
App
├── Layout
│   ├── Header (session info, status)
│   └── MainContent
│       ├── ChatPanel
│       │   ├── MessageList
│       │   ├── MessageInput
│       │   └── ThinkingIndicator
│       └── ToolPanel
│           ├── TerminalView
│           ├── EditorView
│           └── BrowserView
```

### State Management (Zustand)

Global state structure:
- Session ID
- Agent status (idle, thinking, executing)
- Messages array
- Tool executions
- Terminal output
- Editor files
- Active view

### Real-time Communication

**WebSocket (STOMP)** for bidirectional:
- Status updates
- Tool execution events
- Terminal output streams
- File changes

**Server-Sent Events** for unidirectional:
- LLM response streaming
- Token-by-token updates

## Chat Panel Implementation

### Message Types
- User messages
- Agent thoughts
- Code blocks with syntax highlighting
- Tool execution cards
- Error displays

### Streaming Response Pattern
1. Create placeholder message
2. Append tokens as they arrive
3. Show typing indicator
4. Parse markdown and code blocks
5. Update on completion

### Virtual Scrolling
For conversations > 50 messages:
- Use react-window
- Calculate dynamic heights
- Maintain scroll position
- Load on demand

## Terminal Panel

### xterm.js Integration
- Ubuntu-like appearance
- ANSI color support
- Command history
- Output streaming
- Non-interactive (agent-only)

### Output Handling
1. Receive output via WebSocket
2. Parse ANSI escape codes
3. Append to terminal
4. Auto-scroll to bottom
5. Preserve history

## Editor Panel

### Monaco Editor Setup
- Syntax highlighting
- Multiple file tabs
- Read-only mode
- Auto-detect language
- Theme: VS Code Dark

### File Management
- File tree sidebar
- Click to open files
- Show active file
- Display file changes
- Sync with backend

## Browser Panel

### Sandboxed iframe
- Isolated execution
- No external network
- Rendered HTML only
- URL bar simulation
- Navigation disabled

### Content Updates
1. Receive HTML via WebSocket
2. Inject into iframe
3. Update URL display
4. Handle navigation events

## Tool Execution Visualization

### Execution Card States
- Pending (gray, clock icon)
- Running (blue, spinner)
- Success (green, check)
- Failed (red, X)

### Information Display
- Tool name
- Input parameters
- Execution time
- Output/Result
- Error messages

## Animation Patterns

### Smooth Transitions
- Message appearance (fade + slide)
- Tool card expansion
- Panel switching
- Status changes

### Loading States
- Three-dot animation for thinking
- Progress bars for long operations
- Skeleton screens for data
- Spinners for tool execution

## Responsive Design

### Breakpoints
- Mobile (< 768px): Stack panels vertically
- Tablet (768-1024px): Hide sidebar
- Desktop (> 1024px): Full three-panel

### Adaptation Strategy
- Prioritize chat on mobile
- Tool panels in tabs/drawer
- Collapsible sections
- Touch-friendly controls

## WebSocket Connection

### Connection Management
1. Connect on session start
2. Auto-reconnect on disconnect
3. Queue messages when offline
4. Sync on reconnection

### Subscription Topics
- `/topic/agent/{sessionId}/status`
- `/topic/agent/{sessionId}/tools`
- `/topic/agent/{sessionId}/terminal`
- `/topic/agent/{sessionId}/files`

## Performance Optimization

### Key Strategies
- Memoize expensive components
- Debounce user input
- Lazy load heavy components
- Code-split routes
- Virtual scroll long lists

### Monaco/xterm Loading
- Dynamic imports
- Show loading placeholder
- Cache instances
- Dispose on unmount

## Accessibility

### Requirements
- Keyboard navigation
- Screen reader support
- ARIA labels
- Focus management
- High contrast mode

### Implementation
- Semantic HTML
- Role attributes
- Live regions for updates
- Skip navigation links

## Error Handling

### Error Boundaries
- Catch React errors
- Show fallback UI
- Log to backend
- Offer recovery action

### Connection Errors
- Show offline indicator
- Queue user messages
- Retry with backoff
- Notify on reconnection

## Theme System

### Color Scheme
- Light/Dark modes
- System preference detection
- Smooth transitions
- Consistent palette

### Component Styling
- Tailwind utilities
- CSS modules for complex
- Themed variables
- shadcn/ui components

## Testing Approach

### Component Tests
- Render without errors
- User interactions
- State updates
- WebSocket mocking

### E2E Tests
- Full conversation flow
- Tool execution
- Panel interactions
- Error scenarios

## Best Practices

1. **Stream everything** for responsiveness
2. **Virtualize long lists** for performance
3. **Memoize components** to prevent re-renders
4. **Handle disconnections** gracefully
5. **Show loading states** always
6. **Test on mobile** early
7. **Profile performance** regularly

## Common Issues

- WebSocket connection drops
- Large message performance
- Monaco initialization delays
- State synchronization bugs
- Memory leaks from subscriptions

## Implementation Order

1. Basic layout structure
2. Chat with simple messages
3. WebSocket connection
4. Message streaming
5. Terminal integration
6. Editor integration
7. Tool visualizations
8. Browser panel
9. Responsive design
10. Performance optimization