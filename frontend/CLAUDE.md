# Frontend — Module CLAUDE.md

This supplements the root CLAUDE.md. Read that first.

## Identity

React 19 SPA with an eight-panel workspace UI for observing and interacting with the AI agent. Real-time updates arrive via STOMP WebSocket; REST API handles CRUD operations.

## Module-Specific Patterns

- **State flows through one store**: All state lives in `stores/agentStore.ts` (Zustand). Components read via `useAgentStore()` hook selectors. Never use React Context or local state for data shared between panels — add it to the store.
- **WebSocket events drive the UI**: `services/websocket.ts` receives STOMP messages on `/topic/agent/{sessionId}`. These are passed to `agentStore.handleAgentEvent()` which routes by `event.type` (`thought_chunk`, `thought`, `message_chunk`, `message`, `code`, `output`, `error`, `status`). The REST API (`services/api.ts`) is only for initial load and CRUD — live updates come exclusively via WebSocket.
- **Component structure**: Each feature directory (`Chat/`, `Terminal/`, `Browser/`, etc.) contains its components plus a barrel `index.ts`. Tests live in `components/__tests__/`, NOT inside feature directories.
- **No direct API calls from components**: Components call store actions (e.g., `loadSessions()`, `switchSession()`), which internally call `apiService` methods. The only exception is `ChatPanel.tsx` which calls `apiService.chat()` directly for the send-message flow.
- **Tailwind CSS 4 — utility-first, no CSS modules**: All styling uses Tailwind classes inline. The theme is centralized in `theme/theme.ts` which exports color constants and component style maps. Use these theme tokens for consistency, not raw color values.
- **API client is a singleton class**: `apiService` in `services/api.ts` is a class instance, not bare functions. The module also exports top-level function aliases (`chat()`, `listSessions()`, etc.) for backward compatibility with tests. When adding new API calls, add a method to `ApiService` class AND a corresponding function export.

## Test Patterns

- **Test framework**: Vitest + `@testing-library/react` + `jsdom` environment. Setup in `src/test/setup.ts` (imports `@testing-library/jest-dom`). Config in `vite.config.ts` under `test`.
- **Mocking stores**: `vi.mock('../../stores/agentStore')` then cast: `(useAgentStore as any).mockReturnValue({...})`. Return only the selectors that the component under test uses.
- **Mocking services**: `vi.mock('../../services/api', () => ({ apiService: { chat: vi.fn() } }))`. Mock individual methods, not the whole module shape.
- **Mocking child components**: Use `vi.mock('../Chat/MessageList', () => ({ MessageList: ... }))` to replace complex children with simple test doubles. This is the established pattern for isolating component tests.
- **DOM assertions**: Use Testing Library queries (`screen.getByText`, `screen.getByPlaceholderText`) and `@testing-library/jest-dom` matchers (`toBeInTheDocument`). Avoid querying by CSS class — this caused the 3 pre-existing failures in `MessageItem.test.tsx`.

## Key Files

| Task | Start Here |
|---|---|
| Add new panel | Create `components/{Panel}/`, register in `components/Layout/MainLayout.tsx` |
| Add store state | `stores/agentStore.ts` — add to interface + initial state + actions |
| Add API endpoint | `services/api.ts` — add method to `ApiService` class + function export |
| Add WebSocket event type | `agentStore.ts` `handleAgentEvent()` + `types/index.ts` `AgentEvent` |
| Add shared type | `types/index.ts` |
| Theme colors/tokens | `theme/theme.ts` |

## Gotchas

- **Vite proxy**: Dev server proxies `/api` → `localhost:8080` and `/ws` → `localhost:8080` (WebSocket). The backend must be running for the frontend to function beyond static rendering.
- **`npm run build` runs `tsc -b` first**: TypeScript errors block the Vite build. Run `npx tsc -b` separately to get clear type errors before attempting a full build.
- **Pre-existing test failures**: 3 tests in `MessageItem.test.tsx` fail (CSS class selector queries) and 1 in `agentStore.test.ts` (clear session). Do not attempt to fix these unless explicitly asked — they are known baseline failures.
- **Pre-existing lint errors**: ~137 ESLint errors, mostly `@typescript-eslint/no-explicit-any`. Do not introduce new `any` types, but do not fix pre-existing ones unless asked.
