# Flow: Tool Execution (Python Sandbox → Java Tool → Result)

Tools are Java components that the LLM invokes indirectly by writing Python code. The sandbox executor intercepts tool calls via a stdin/stdout RPC protocol and dispatches them to Java.

## How the LLM Sees Tools

At the start of each iteration, `PromptBuilder.buildSystemPrompt()` includes tool descriptions from `ToolRegistry.getToolDescriptions()`. Example:

```
Available tools:
- file_read: Read a file. Signature: file_read(sessionId: str, path: str) -> str
- shell_exec: Run a shell command. Signature: shell_exec(sessionId: str, command: str, timeout: int = 30) -> dict
```

The LLM writes Python code that calls these functions as if they were native Python:

```python
result = file_read(path="data.csv")
print(result)
```

## How Tool Stubs Are Generated

`ToolRegistry.generatePythonBindings()` produces Python function stubs for each registered tool:

```python
def file_read(sessionId: str, path: str):
    '''Read the entire contents of a file.'''
    if 'sessionId' not in locals() or sessionId is None:
        sessionId = SESSION_ID    # auto-injected global
    return _execute_tool('file_read', locals())
```

The `_execute_tool` function handles the RPC:

```python
def _execute_tool(tool_name, params):
    request = {"id": str(uuid.uuid4()), "tool": tool_name, "params": params}
    print("__TOOL_REQUEST__" + json.dumps(request) + "__END__")
    sys.stdout.flush()
    response_line = input()  # blocks until Java writes response
    # parse __TOOL_RESPONSE__{json}__END__
    return result_dict
```

## RPC Protocol

```
┌──────────────────┐          ┌──────────────────────┐
│  Python Sandbox  │          │  Java (ToolRpcHandler)│
│                  │          │                       │
│  _execute_tool() │          │                       │
│   │              │          │                       │
│   ├─ print       │ stdout → │ read line             │
│   │  __TOOL_     │ ──────── │  │                    │
│   │  REQUEST__   │          │  ├─ extractJson()     │
│   │  {json}      │          │  ├─ toolRegistry      │
│   │  __END__     │          │  │    .getTool(name)   │
│   │              │          │  ├─ tool.execute(      │
│   │              │          │  │     params)         │
│   │              │          │  ├─ stateService       │
│   │  input()     │ stdin  ← │  │    .recordTool     │
│   │  (blocks)    │ ──────── │  │    Execution()     │
│   │              │          │  └─ write              │
│   ├─ parse       │          │     __TOOL_RESPONSE__ │
│   │  response    │          │     {json}__END__     │
│   └─ return dict │          │                       │
└──────────────────┘          └──────────────────────┘
```

Request JSON: `{ "id": "uuid", "tool": "file_read", "params": { "sessionId": "abc", "path": "data.csv" } }`

Response JSON: `{ "id": "uuid", "result": { "success": true, "content": "..." }, "error": null }`

## Tool Categories (25 tools total)

### File Operations (6 tools, extend `FileTool`)
Sandboxed to `{workspace}/{sessionId}/`. Path traversal blocked by `validateAndResolvePath()`.

| Tool | Purpose |
|---|---|
| `file_read` | Read file content |
| `file_write` | Write/overwrite file |
| `file_list` | Directory tree listing (max depth 10) |
| `file_find_by_name` | Find files by glob or regex (max 100 results) |
| `file_find_content` | Full-text grep with 2-line context |
| `file_replace_string` | Find-and-replace all occurrences |

### Shell Execution (1 tool)
| Tool | Purpose |
|---|---|
| `shell_exec` | Run bash command in sandbox (30s timeout) |

Registered manually via `ToolConfiguration` (not `@Component`) to break a circular dependency: `SandboxExecutor` → `ToolRegistry` → `ShellExecTool` → `SandboxExecutor`.

### Browser Automation (8 tools, extend `BrowserTool`)
Session-scoped headless Chromium via Playwright.

| Tool | Purpose |
|---|---|
| `browser_navigate` | Navigate to URL |
| `browser_view` | Screenshot + accessibility tree + HTML |
| `browser_click` | Click element by CSS selector |
| `browser_input` | Type into input field |
| `browser_press_key` | Press keyboard key |
| `browser_scroll_down/up` | Scroll page |
| `browser_refresh` | Reload page |

### Web Search (1 tool)
| Tool | Purpose |
|---|---|
| `search_web` | SerpAPI → Google Custom Search → browser guidance fallback |

### MCP Integration (2 tools)
| Tool | Purpose |
|---|---|
| `search_tools` | Keyword search over MCP server tool catalogs |
| `mcp_call` | Execute any MCP tool by name with arbitrary params |

### Communication (2 tools)
| Tool | Purpose |
|---|---|
| `message_notify_user` | Push info/warning notification to user |
| `message_ask_user` | Pause agent, set WAITING_INPUT, wait for user reply |

### Agent Utility (3 tools)
| Tool | Purpose |
|---|---|
| `todo` | Read/write `todo.md` plan file |
| `visualize_data` | Returns chart code guidance (matplotlib/seaborn/plotly) |
| `print_message` | Simple echo (testing) |

## Security Model

### Workspace Isolation (`FileTool.validateAndResolvePath`)

```java
Path resolved = sessionWorkspace.resolve(filePath).normalize();
if (!resolved.startsWith(sessionWorkspace)) {
    throw new SecurityException("Access denied: Path escapes session workspace.");
}
```

Properties enforced:
1. **Session isolation** — each session's files are in `{workspace}/{sessionId}/`
2. **Path traversal prevention** — `normalize()` resolves `../`, `startsWith()` catches escapes
3. **Symlink resistance** — `toRealPath()` dereferences symlinks on the workspace root

### Code Validation (`PythonValidationService`)

**Syntax**: `python3 -c "import ast; ast.parse(code)"` — hard failure on syntax error
**Safety**: Regex blocks `rm -rf` and `__import__('os').system`. Warns (but allows) `eval()`, `exec()`, `shutil.rmtree`.

### Network Access (`requiresNetwork()`)

Tools declare whether they need network. Docker sandbox runs with `network-mode=none` by default. Browser and web search tools set `requiresNetwork() = true`.

## Error Handling

Tool errors never crash the agent loop. The pipeline:
1. `Tool.execute()` throws → `ToolRpcHandler` catches, returns `{ error: "..." }` to Python
2. Python stub raises the error → user code's `try/except` may catch it
3. If uncaught → Python process prints `ERROR: ...` to stderr
4. Java detects stderr error → `ExecutionResult.success = false`
5. Error becomes an OBSERVATION event → LLM sees it on next iteration and can self-correct
