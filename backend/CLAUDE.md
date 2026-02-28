# Backend — Module CLAUDE.md

This supplements the root CLAUDE.md. Read that first.

## Identity

Spring Boot 3.5.6 monolith implementing a CodeAct agent loop. The core cycle is: user message → LLM generates Python code → sandbox executes it → observation fed back to LLM → repeat (up to 20 iterations).

## Module-Specific Patterns

- **Tool implementation contract**: New tools MUST extend an abstract base class or implement `Tool` directly. File tools extend `FileTool` (handles path sandboxing via `validateAndResolvePath`). Browser tools extend the browser tool base. All tools receive and return `Map<String, Object>` — use the `success(message)` and `error(message, exception)` helper methods from `FileTool` for consistent response shapes.
- **Tool Python signatures**: Every `Tool` must define `getPythonSignature()` returning the exact Python function signature (e.g., `file_read(sessionId: str, path: str) -> str`). `ToolRegistry.generatePythonBindings()` uses this to auto-generate Python stubs injected into the sandbox. An incorrect signature breaks the agent's ability to call that tool.
- **Sandbox executor abstraction**: `SandboxExecutor` is an interface with two implementations — `PythonSandboxExecutor` (Docker, default profile) and `HostPythonExecutor` (dev profile). When testing sandbox-related code, mock `PythonSandboxExecutor`, not `HostPythonExecutor`.
- **Spring profiles for tests**: Unit tests use `@ExtendWith(MockitoExtension.class)` with `@Mock`/`@InjectMocks`. Integration tests use `@SpringBootTest` + `@ActiveProfiles("test")` + `@Import(IntegrationTestConfiguration.class)` + `@AutoConfigureTestDatabase(replace = ANY)`. The `IntegrationTestConfiguration` provides a mocked `AnthropicService` that returns canned responses.
- **`@Value` fields in tests**: Services with `@Value` fields (like `maxIterations` in `CodeActAgentService`) require `ReflectionTestUtils.setField()` in test setup when using `@InjectMocks`.
- **Event stream ordering**: Events must be appended through `EventService` methods (`appendUserMessage`, `appendAgentThought`, `appendAgentAction`, `appendObservation`, `appendAgentResponse`) — never write directly to `EventRepository`. These methods assign correct `iteration` and `sequence` numbers.
- **Streaming responses**: `AnthropicService.generateStream()` returns `Flux<String>`. The agent loop in `CodeActAgentService` collects these chunks and broadcasts them via `SimpMessagingTemplate` to `/topic/agent/{sessionId}`. Changes to streaming must preserve the chunk-by-chunk WebSocket emission pattern.
- **`mvnw` is not a real wrapper**: It's `exec mvn "$@"`. System Maven is required on PATH.

## Key Entry Points

| Task | Start Here |
|---|---|
| Agent loop logic | `CodeActAgentService.processQuery()` |
| LLM prompt construction | `PromptBuilder.buildSystemPrompt()` |
| Code block extraction | `PromptBuilder.extractCodeBlocks()` |
| Code execution | `PythonSandboxExecutor.execute()` / `HostPythonExecutor.execute()` |
| Tool RPC from sandbox | `ToolRpcHandler` |
| WebSocket event emission | `WebSocketController` + `SimpMessagingTemplate` |
| Session state persistence | `AgentStateService` |
| Event sourcing | `EventService` |

## Test Structure

| Pattern | Location | Annotations |
|---|---|---|
| Controller unit tests | `test/.../controller/` | `@WebMvcTest(XController.class)` + `@Import(TestSecurityConfig.class)` |
| Service unit tests | `test/.../service/` | `@ExtendWith(MockitoExtension.class)` |
| Integration tests | `test/.../integration/` | `@SpringBootTest` + `@ActiveProfiles("test")` + `@Import(IntegrationTestConfiguration.class)` |
| Model tests | `test/.../model/` | Plain JUnit 5 |
| Tool tests | `test/.../tool/impl/` | `@ExtendWith(MockitoExtension.class)` |

## Gotchas

- `schema.sql` initializes H2 in dev/test profiles. If you add a new entity, you must also add its `CREATE TABLE` to `schema.sql` or the dev profile breaks.
- Both `application.yml` and `application.properties` exist. Spring merges both, with `.properties` winning on conflicts. Prefer editing `.yml` for new settings.
- The `@Async` annotation on `MyManusApplication` means `processQuery` can run asynchronously — be aware of thread safety in services that hold mutable state.
