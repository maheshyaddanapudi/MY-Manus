---
paths:
- 'frontend/src/components/**/*.tsx'
- 'frontend/src/components/**/*.ts'
---

# Frontend Component Rules

ALWAYS use functional components with hooks. No class components exist in this codebase.

ALWAYS place new components in a feature directory under `components/` (e.g., `components/MyFeature/MyComponent.tsx`). Create a barrel `index.ts` that re-exports the component:
```typescript
// components/MyFeature/index.ts
export { MyComponent } from './MyComponent';
```

ALWAYS read state from `useAgentStore()` with selectors. Destructure only the fields the component needs:
```typescript
const { sessionId, messages, addMessage } = useAgentStore();
```
NEVER call `useAgentStore.getState()` in render — it bypasses React's re-render subscription.

ALWAYS use Tailwind CSS utility classes for styling. Reference `theme/theme.ts` for color tokens. The project uses a purple/blue gradient theme (`from-gray-900`, `bg-purple-600`, `text-purple-400`). NEVER add inline `style={}` objects or CSS modules.

ALWAYS register new panels in `components/Layout/MainLayout.tsx`. The `activePanel` state in `agentStore` controls which panel is visible. Valid panel types are defined in the `AgentState` interface: `'terminal' | 'editor' | 'browser' | 'events' | 'files' | 'replay' | 'knowledge' | 'plan'`.

NEVER add new shared TypeScript types in component files. All shared types (interfaces, type aliases) go in `types/index.ts`.

ALWAYS handle the loading/error states in components that call async store actions. Follow the pattern in `ChatPanel.tsx`: track `isProcessing` locally, set `agentStatus` via store action, and add error messages to the message list.
