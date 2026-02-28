---
paths:
- 'backend/src/main/java/ai/mymanus/tool/**'
---

# Tool Implementation Rules

ALWAYS implement new tools by following this contract:

1. Create a `@Component` class in the appropriate `tool/impl/` subdirectory (`file/`, `browser/`, `shell/`, `communication/`, or root for general tools).
2. File tools MUST extend `FileTool` — it provides `validateAndResolvePath()` for sandbox path security, and `success()`/`error()` for consistent response maps.
3. Non-file tools implement `Tool` interface directly.

ALWAYS implement all four `Tool` interface methods:
- `getName()` — snake_case tool name matching the Python function name (e.g., `file_read`)
- `getDescription()` — one-line description injected into sandbox Python docstrings
- `getPythonSignature()` — exact Python function signature (e.g., `file_read(sessionId: str, path: str) -> str`). This generates the Python binding in `ToolRegistry.generatePythonBindings()`. A wrong signature silently breaks the agent's ability to call the tool.
- `execute(Map<String, Object> parameters)` — receives parameters by name from sandbox Python code

ALWAYS return `Map<String, Object>` from `execute()` with at minimum `success` (boolean) and either `message` or `error` (string). Use the inherited `success()` and `error()` helpers when extending `FileTool`.

NEVER register tools manually — `ToolRegistry` auto-discovers all `@Component` beans implementing `Tool` via Spring DI constructor injection.

ALWAYS include `sessionId` as the first parameter in `getPythonSignature()` when the tool needs workspace isolation. The sandbox auto-injects `SESSION_ID` via `ToolRegistry.generatePythonBindings()`.

ALWAYS set `requiresNetwork()` to return `true` for tools that make HTTP calls (e.g., `WebSearchTool`). The sandbox uses this to decide network access.
