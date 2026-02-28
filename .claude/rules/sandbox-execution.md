---
paths:
- 'backend/src/main/java/ai/mymanus/service/sandbox/**'
---

# Sandbox Execution Rules

This is the highest-churn backend directory (15 commits, 7 on HostPythonExecutor alone). Changes here cause subtle state persistence and serialization bugs.

ALWAYS null-check `getStdout()` and `getStderr()` on `ExecutionResult` before calling `.contains()`, `.length()`, or any string method. These fields can be null and have caused NullPointerExceptions in production (commit `3617575`).

NEVER serialize Python functions, modules, or class objects into execution context variables. The state persistence logic captures Python variables between iterations — only JSON-serializable primitives, lists, and dicts survive. This caused commit `1b812f0` ("exclude functions and modules from state capture").

ALWAYS use `json.loads()` for state restoration, NEVER `ast.literal_eval()`. The latter cannot handle `null`, `true`, `false` (JSON values vs Python values). This caused commit `b742c04`.

ALWAYS build `ExecutionResult` using the Lombok `@Builder`:
```java
ExecutionResult.builder()
    .stdout(output)
    .stderr(errors)
    .exitCode(code)
    .success(code == 0)
    .variables(capturedVars)
    .executionTimeMs(elapsed)
    .build();
```

NEVER modify `PythonSandboxExecutor` without understanding `@ConditionalOnProperty(name = "sandbox.mode", havingValue = "docker")` — it only activates when Docker mode is configured. `HostPythonExecutor` activates with `havingValue = "host"`. Tests should mock `PythonSandboxExecutor` (the Docker variant), not `HostPythonExecutor`.

ALWAYS preserve the tool RPC pattern: sandbox Python calls `_execute_tool(name, params)` → `ToolRpcHandler` dispatches to `ToolRegistry.getTool(name)` → returns JSON result. Do not bypass this path.
