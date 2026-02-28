---
paths:
- 'frontend/src/stores/**'
- 'frontend/src/services/websocket.ts'
---

# Frontend State Management Rules

`agentStore.ts` is the third highest-churn file (6 commits) and the single source of truth for all application state.

NEVER modify `handleAgentEvent()` without understanding the chunk buffering system. It maintains five interdependent tracking variables:
- `thoughtBuffer` / `lastThoughtMessageId` / `lastEventId` — accumulate streaming thought chunks
- `messageBuffer` / `lastMessageId` — accumulate streaming message chunks

These are reset to `''`/`null` when a `complete` event arrives. Breaking this causes duplicate messages, lost thoughts, or orphaned events in the UI.

ALWAYS add new shared state to the `AgentState` interface in `agentStore.ts`, NOT to component-local state. Both the interface type definition and the initial state value must be added.

ALWAYS add new async operations as store actions (like `loadSessions`, `switchSession`) that internally call `apiService` methods. Components should call store actions, not `apiService` directly. The one exception is `ChatPanel.tsx` which calls `apiService.chat()` directly.

NEVER clear `events` array in `resetSession()` or `clearSession()`. Events persist for the Event Stream tab across session switches. This is intentional — see the inline comments.

ALWAYS update the `AgentEvent` type in `types/index.ts` when adding a new WebSocket event type, then add the handler case in `handleAgentEvent()`.

NEVER modify `websocket.ts` STOMP subscription topic pattern (`/topic/agent/{sessionId}`) without also updating `WebSocketController.java` on the backend — they must match exactly.
