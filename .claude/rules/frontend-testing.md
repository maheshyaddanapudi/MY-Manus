---
paths:
- 'frontend/src/components/__tests__/**'
- 'frontend/src/services/__tests__/**'
- 'frontend/src/stores/__tests__/**'
---

# Frontend Testing Rules

`components/__tests__/` is the highest-touch frontend directory (21 file touches). Follow these exact patterns.

ALWAYS mock `useAgentStore` by calling `vi.mock('../../stores/agentStore')` at module scope, then in `beforeEach`: `(useAgentStore as any).mockReturnValue({ sessionId: 'test-session-123', messages: [], addMessage: vi.fn() })`. Return ONLY the selectors the component under test uses.

ALWAYS mock `apiService` as: `vi.mock('../../services/api', () => ({ apiService: { chat: vi.fn() } }))`. Mock individual methods, not the full module shape.

ALWAYS mock complex child components to isolate tests: `vi.mock('../Chat/MessageList', () => ({ MessageList: ({ messages }: any) => <div data-testid="message-list">{...}</div> }))`. This is the established pattern.

NEVER query DOM elements by CSS class name (e.g., `.justify-start`, `.justify-end`). This caused all 3 pre-existing test failures in `MessageItem.test.tsx`. Use Testing Library queries instead: `screen.getByText()`, `screen.getByTestId()`, `screen.getByRole()`, `screen.getByPlaceholderText()`.

ALWAYS call `vi.clearAllMocks()` in `beforeEach` to prevent mock state leaking between tests.

ALWAYS mock `Element.prototype.scrollIntoView = vi.fn()` in `beforeAll` when testing components that render `MessageList` — jsdom does not implement `scrollIntoView`.
