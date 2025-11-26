# Tools Development Guide

## Overview

Tools in the Manus AI clone are Java components that expose functionality to the agent as Python functions. Tools are automatically discovered via Spring's dependency injection and registered in the ToolRegistry.

## Tool Architecture

### Core Interface

**Location**: `/backend/src/main/java/ai/mymanus/tool/Tool.java`

```java
public interface Tool {
    /**
     * Get the name of the tool (used in Python function name)
     */
    String getName();

    /**
     * Get a description of what the tool does
     */
    String getDescription();

    /**
     * Get the Python function signature
     */
    String getPythonSignature();

    /**
     * Execute the tool with given parameters
     * @param parameters Map of parameter name to value
     * @return Result of tool execution (must be JSON-serializable)
     */
    Map<String, Object> execute(Map<String, Object> parameters) throws Exception;

    /**
     * Whether this tool requires network access
     */
    default boolean requiresNetwork() {
        return false;
    }
}
```

Every tool must:
1. Implement the `Tool` interface
2. Be a Spring `@Component` for auto-discovery
3. Return JSON-serializable Map from `execute()`
4. Handle errors gracefully

## Implemented Tools

### File Operations

**Base Class**: `/backend/src/main/java/ai/mymanus/tool/impl/file/FileTool.java`

All file tools extend `FileTool` which provides:
- Security sandboxing (restricts access to workspace)
- Path validation (prevents path traversal)
- Helper methods (`success()`, `error()`, `addFileMetadata()`)

#### Security Model

```java
// Workspace root for file operations
protected static final String WORKSPACE_ROOT = System.getenv()
    .getOrDefault("MANUS_WORKSPACE", "/tmp/manus-workspace");

protected Path validateAndResolvePath(String filePath) {
    Path workspaceRoot = Paths.get(WORKSPACE_ROOT).toRealPath();
    Path resolvedPath = workspaceRoot.resolve(filePath).normalize();

    // Security check: resolved path must start with workspace root
    if (!resolvedPath.startsWith(workspaceRoot)) {
        throw new SecurityException("Access denied: Path escapes workspace");
    }

    return resolvedPath;
}
```

**Implemented File Tools**:
- `file_read(path: str)` - Read file contents
- `file_write(path: str, content: str)` - Write/create file
- `file_list(path: str)` - List directory contents
- `file_find_by_name(pattern: str, path: str)` - Search files by name
- `file_find_content(pattern: str, path: str)` - Search file contents
- `file_replace_string(path: str, old_string: str, new_string: str)` - Replace text

**Example**: FileReadTool

```java
@Slf4j
@Component
public class FileReadTool extends FileTool {

    @Override
    public String getName() {
        return "file_read";
    }

    @Override
    public String getDescription() {
        return "Read the entire contents of a file. Returns the file content as a string.";
    }

    @Override
    public String getPythonSignature() {
        return "file_read(path: str) -> str";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String filePath = (String) parameters.get("path");

        // Validate and resolve path with security checks
        Path resolvedPath = validateAndResolvePath(filePath);

        // Check if file exists
        if (!Files.exists(resolvedPath)) {
            return error("File not found: " + filePath, null);
        }

        // Read file content
        String content = Files.readString(resolvedPath, StandardCharsets.UTF_8);

        // Build result
        var result = success("File read successfully");
        result.put("path", filePath);
        result.put("content", content);
        result.put("length", content.length());
        addFileMetadata(result, resolvedPath);

        return result;
    }
}
```

### Browser Automation

**Base Class**: `/backend/src/main/java/ai/mymanus/tool/impl/browser/BrowserTool.java`

Browser tools use Playwright for browser automation and inherit from `BrowserTool`.

**Implemented Browser Tools**:
- `browser_navigate(sessionId: str, url: str)` - Navigate to URL
- `browser_view(sessionId: str)` - Get current page content
- `browser_click(sessionId: str, selector: str)` - Click element
- `browser_input(sessionId: str, selector: str, text: str)` - Type text
- `browser_scroll_up(sessionId: str)` - Scroll up
- `browser_scroll_down(sessionId: str)` - Scroll down
- `browser_press_key(sessionId: str, key: str)` - Press keyboard key
- `browser_refresh(sessionId: str)` - Refresh page

**Example**: BrowserNavigateTool

```java
@Slf4j
@Component
public class BrowserNavigateTool extends BrowserTool {

    public BrowserNavigateTool(BrowserExecutor browserExecutor) {
        super(browserExecutor);
    }

    @Override
    public String getName() {
        return "browser_navigate";
    }

    @Override
    public String getDescription() {
        return "Navigate the browser to a specific URL. Returns success status and current URL.";
    }

    @Override
    public String getPythonSignature() {
        return "browser_navigate(sessionId: str, url: str) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = getSessionId(parameters);
        String url = (String) parameters.get("url");

        if (url == null || url.isEmpty()) {
            return error("URL parameter is required", null);
        }

        browserExecutor.navigate(sessionId, url);

        var result = success("Navigation successful");
        result.put("url", browserExecutor.getCurrentUrl(sessionId));
        result.put("title", browserExecutor.getCurrentTitle(sessionId));

        return result;
    }
}
```

### Shell Execution

**Location**: `/backend/src/main/java/ai/mymanus/tool/impl/shell/ShellExecTool.java`

Executes shell commands in the sandbox using the CodeAct pattern.

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class ShellExecTool implements Tool {

    private final SandboxExecutor sandboxExecutor;

    @Override
    public String getName() {
        return "shell_exec";
    }

    @Override
    public String getDescription() {
        return "Execute a shell command in the sandbox. Returns stdout, stderr, and exit code.";
    }

    @Override
    public String getPythonSignature() {
        return "shell_exec(sessionId: str, command: str, timeout: int = 30) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        String sessionId = (String) parameters.get("sessionId");
        String command = (String) parameters.get("command");

        // Generate Python code to execute shell command (CodeAct pattern)
        String pythonCode = generateShellPython(command, timeout);

        // Execute Python code in sandbox
        var execResult = sandboxExecutor.execute(sessionId, pythonCode, Map.of());

        var response = new HashMap<String, Object>();
        response.put("success", execResult.isSuccess());
        response.put("stdout", execResult.getStdout());
        response.put("stderr", execResult.getStderr());
        response.put("exitCode", execResult.isSuccess() ? 0 : 1);

        return response;
    }

    private String generateShellPython(String command, int timeout) {
        return String.format("""
            import subprocess
            import sys

            try:
                result = subprocess.run(
                    '%s',
                    shell=True,
                    capture_output=True,
                    text=True,
                    timeout=%d
                )
                print(result.stdout, end='')
                sys.exit(result.returncode)
            except subprocess.TimeoutExpired:
                print('Command timed out', file=sys.stderr)
                sys.exit(124)
            """, command, timeout);
    }
}
```

**Key Pattern**: Tools generate Python code that's executed in the sandbox (CodeAct approach).

### Web Search

**Location**: `/backend/src/main/java/ai/mymanus/tool/impl/WebSearchTool.java`

Performs web searches using a multi-tier strategy with automatic fallback:

**Search Strategies** (in priority order):

1. **SerpAPI** (if `search.api.serp-api-key` configured)
   - Most reliable commercial search API
   - Costs money but provides high-quality results
   - URL: https://serpapi.com

2. **Google Custom Search API** (if `search.api.google-api-key` and `search.api.google-cx` configured)
   - High-quality results from Google
   - Free tier available
   - URL: https://developers.google.com/custom-search

3. **Browser Guidance Fallback** (always available)
   - Intelligent fallback when no API key configured
   - Provides multiple search engine options (Google, Bing, DuckDuckGo, Brave)
   - Instructs agent to use browser tools to search ANY website
   - Agent chooses search engine and uses `browser_navigate`, `browser_view`, etc.
   - Not hardcoded scraping - true browser tool delegation

```java
@Slf4j
@Component
public class WebSearchTool implements Tool {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${search.api.serp-api-key:}")
    private String serpApiKey;

    @Value("${search.api.google-api-key:}")
    private String googleApiKey;

    @Value("${search.api.google-cx:}")
    private String googleCx;

    @Override
    public String getName() {
        return "search_web";
    }

    @Override
    public String getPythonSignature() {
        return "search_web(query: str, max_results: int = 5) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        // Try SerpAPI → Google API → Browser Guidance
        // Returns search results or guidance for using browser tools
    }

    private Map<String, Object> provideBrowserGuidance(String query, int maxResults) {
        // Returns multiple search engine URLs and browser tool sequence
        // Lets agent choose which search engine to use
    }
}
```

**Configuration** (`application.yml`):

```yaml
search:
  api:
    serp-api-key: ${SERP_API_KEY:}         # Optional: SerpAPI key
    google-api-key: ${GOOGLE_API_KEY:}     # Optional: Google Custom Search key
    google-cx: ${GOOGLE_CX:}               # Optional: Google Custom Search CX
```

**Usage Examples**:

```python
# With API key configured - returns direct results
results = search_web(query="latest AI news", max_results=5)
# Returns: {"success": true, "method": "serpapi", "results": [...]}

# Without API key - provides browser guidance
results = search_web(query="Python tutorials")
# Returns: {
#   "success": true,
#   "method": "browser-guidance",
#   "search_engine_options": [
#     {"engine": "Google", "url": "https://www.google.com/search?q=..."},
#     {"engine": "DuckDuckGo", "url": "https://duckduckgo.com/?q=..."},
#     {"engine": "Bing", "url": "https://www.bing.com/search?q=..."},
#     {"engine": "Brave Search", "url": "https://search.brave.com/search?q=..."}
#   ],
#   "browser_tool_sequence": [
#     "1. Choose a search engine from the options above",
#     "2. Use browser_navigate(url) to navigate to the search engine URL",
#     "3. Use browser_view() to see the search results page",
#     "4. Use browser_click() or browser_extract() to interact with results",
#     "5. Extract the information you need"
#   ]
# }
```

**Note**: Fully implemented with production-ready fallback strategy. Always works even without API keys.

### Communication Tools

**User Notification**: `/backend/src/main/java/ai/mymanus/tool/impl/communication/MessageNotifyUserTool.java`

```java
@Component
public class MessageNotifyUserTool implements Tool {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public String getName() {
        return "message_notify_user";
    }

    @Override
    public String getDescription() {
        return "Send a notification message to the user via WebSocket";
    }

    @Override
    public String getPythonSignature() {
        return "message_notify_user(sessionId: str, message: str, level: str = 'info')";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) {
        String sessionId = (String) parameters.get("sessionId");
        String message = (String) parameters.get("message");
        String level = (String) parameters.getOrDefault("level", "info");

        // Send via WebSocket
        messagingTemplate.convertAndSend("/topic/notifications/" + sessionId,
            Map.of("message", message, "level", level));

        return Map.of("success", true);
    }
}
```

**User Input**: `/backend/src/main/java/ai/mymanus/tool/impl/communication/MessageAskUserTool.java`

Sends a question to the user and waits for a response via WebSocket.

### Data Visualization

**Location**: `/backend/src/main/java/ai/mymanus/tool/impl/DataVisualizationTool.java`

Provides guidance for creating data visualizations using Python libraries.

**Design**: This is a guidance tool (similar to web search browser guidance) that directs the agent to write Python code using matplotlib, seaborn, or plotly. The actual visualization is created by the agent's Python code execution in the sandbox.

**Supported Chart Types**:
- Line charts (`matplotlib.pyplot.plot()` or `seaborn.lineplot()`)
- Bar charts (`matplotlib.pyplot.bar()` or `seaborn.barplot()`)
- Scatter plots (`matplotlib.pyplot.scatter()` or `seaborn.scatterplot()`)
- Histograms (`matplotlib.pyplot.hist()` or `seaborn.histplot()`)
- Heatmaps (`seaborn.heatmap()`)

**Usage Example**:

```python
# Agent calls visualize_data to get guidance
guidance = visualize_data(chart_type="line", data_description="sales over time")
# Returns: {
#   "success": true,
#   "chartType": "line",
#   "guidance": "Use matplotlib.pyplot.plot() or seaborn.lineplot()..."
# }

# Then agent writes Python code based on guidance
# execute_python("""
# import matplotlib.pyplot as plt
# plt.plot(months, sales)
# plt.xlabel('Month')
# plt.ylabel('Sales')
# plt.title('Sales Over Time')
# plt.savefig('/workspace/sales_chart.png')
# """)
```

**Note**: Fully implemented as a CodeAct guidance tool. Works seamlessly with the Python sandbox.

### Utility Tools

**PrintTool**: Simple console output tool
**TodoTool**: Task list management

### Tool Search

**Location**: `/backend/src/main/java/ai/mymanus/tool/impl/SearchToolsTool.java`

Searches across all 22 core infrastructure tools to find relevant tools by capability description.

**Features**:
- Keyword matching with term frequency weighting
- Scores tools by relevance to query
- Returns top-k most relevant tools with descriptions and signatures
- Useful for discovering which tools to use for a specific task

**Scoring Algorithm**:
- **Name match**: 10x weight (highest priority)
- **Signature match**: 5x weight (high priority)
- **Description match**: 2x weight (medium priority)
- **Phrase match bonus**: 15x for exact multi-word phrase matches
- Normalized by query length for fair comparison

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class SearchToolsTool implements Tool {

    private final ToolRegistry toolRegistry;

    @Override
    public String getName() {
        return "search_tools";
    }

    @Override
    public String getPythonSignature() {
        return "search_tools(query: str, top_k: int = 5) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        // Get all tools from registry
        // Score by relevance to query
        // Return top-k matches with descriptions
    }

    private double calculateRelevance(Tool tool, String query) {
        // Keyword matching with weighted scoring
    }
}
```

**Usage Examples**:

```python
# Find file-related tools
results = search_tools(query="read files", top_k=3)
# Returns: {
#   "success": true,
#   "query": "read files",
#   "tools_found": 3,
#   "total_tools": 22,
#   "tools": [
#     {
#       "name": "file_read",
#       "description": "Read file contents...",
#       "signature": "file_read(path: str) -> str",
#       "relevance_score": "15.21"
#     },
#     {
#       "name": "file_list",
#       "description": "List files in directory...",
#       "signature": "file_list(path: str) -> list",
#       "relevance_score": "8.94"
#     },
#     ...
#   ]
# }

# Find browser automation tools
results = search_tools(query="browser automation")
# Returns tools like browser_navigate, browser_view, browser_click, etc.

# Find code execution tools
results = search_tools(query="execute python code")
# Returns tools like execute_python, code_executor, etc.
```

**Note**: Fully implemented with production-ready keyword matching algorithm.

## Tool Registry

**Location**: `/backend/src/main/java/ai/mymanus/tool/ToolRegistry.java`

```java
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        // Auto-registers all Spring @Component tools
        toolList.forEach(tool -> {
            tools.put(tool.getName(), tool);
            log.info("Registered tool: {} - {}", tool.getName(), tool.getDescription());
        });
    }

    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<Tool> getAllTools() {
        return tools.values();
    }

    public String getToolDescriptions() {
        return tools.values().stream()
            .map(tool -> String.format("%s: %s\nSignature: %s",
                tool.getName(),
                tool.getDescription(),
                tool.getPythonSignature()))
            .collect(Collectors.joining("\n\n"));
    }

    public String generatePythonBindings() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Tool functions available in this environment\n\n");

        for (Tool tool : tools.values()) {
            sb.append(String.format("def %s:\n", tool.getPythonSignature()));
            sb.append(String.format("    '''%s'''\n", tool.getDescription()));
            sb.append(String.format("    return _execute_tool('%s', locals())\n\n",
                tool.getName()));
        }

        return sb.toString();
    }
}
```

**Auto-Discovery**: Tools are automatically registered when Spring creates them as @Component beans.

## Creating a New Tool

### Step 1: Implement Tool Interface

```java
package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class MyCustomTool implements Tool {

    @Override
    public String getName() {
        return "my_custom_tool";
    }

    @Override
    public String getDescription() {
        return "Does something useful for the agent";
    }

    @Override
    public String getPythonSignature() {
        return "my_custom_tool(param1: str, param2: int = 10)";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> parameters) throws Exception {
        // Extract parameters
        String param1 = (String) parameters.get("param1");
        Integer param2 = parameters.containsKey("param2")
            ? ((Number) parameters.get("param2")).intValue()
            : 10;

        log.info("Executing custom tool with param1={}, param2={}", param1, param2);

        try {
            // Tool logic here
            String result = performOperation(param1, param2);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("result", result);
            return response;

        } catch (Exception e) {
            log.error("Tool execution failed", e);

            // Return error response
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @Override
    public boolean requiresNetwork() {
        return false; // Set to true if tool needs network access
    }

    private String performOperation(String param1, int param2) {
        // Implementation
        return "result";
    }
}
```

### Step 2: That's It!

Spring automatically discovers and registers your tool. No manual registration needed.

### Step 3: Test Your Tool

```java
@Test
void testCustomTool() {
    MyCustomTool tool = new MyCustomTool();

    Map<String, Object> params = Map.of("param1", "test", "param2", 20);
    Map<String, Object> result = tool.execute(params);

    assertTrue((Boolean) result.get("success"));
    assertNotNull(result.get("result"));
}
```

## Parameter Handling Best Practices

### Type Conversion

```java
// String parameter
String strParam = (String) parameters.get("name");

// Integer parameter with default
Integer intParam = parameters.containsKey("count")
    ? ((Number) parameters.get("count")).intValue()
    : 10;

// Boolean parameter
Boolean boolParam = parameters.containsKey("enabled")
    ? (Boolean) parameters.get("enabled")
    : false;

// List parameter
@SuppressWarnings("unchecked")
List<String> listParam = (List<String>) parameters.get("items");

// Map parameter
@SuppressWarnings("unchecked")
Map<String, Object> mapParam = (Map<String, Object>) parameters.get("data");
```

### Validation

```java
// Required parameter
if (param == null || param.isEmpty()) {
    return error("Parameter 'name' is required", null);
}

// Range validation
if (count < 1 || count > 100) {
    return error("Parameter 'count' must be between 1 and 100", null);
}

// Format validation
if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
    return error("Invalid email format", null);
}
```

## Error Handling Patterns

### Standard Error Response

```java
protected Map<String, Object> error(String message, Exception e) {
    Map<String, Object> result = new HashMap<>();
    result.put("success", false);
    result.put("error", message);
    if (e != null) {
        result.put("errorType", e.getClass().getSimpleName());
        result.put("errorMessage", e.getMessage());
    }
    return result;
}
```

### Graceful Failure

```java
try {
    // Risky operation
    String data = externalApiCall();
    return success("Data retrieved successfully");
} catch (TimeoutException e) {
    log.warn("API timeout, using cached data");
    String cachedData = getFromCache();
    return success("Using cached data (API unavailable)");
} catch (Exception e) {
    log.error("Failed to retrieve data", e);
    return error("Data retrieval failed: " + e.getMessage(), e);
}
```

## Security Considerations

### Path Validation (File Tools)

Always use `validateAndResolvePath()` from `FileTool` base class to prevent path traversal attacks.

### Input Sanitization (Shell Tools)

```java
// Escape single quotes
String escapedCommand = command.replace("'", "\\'");

// Or use prepared statements for SQL
PreparedStatement stmt = connection.prepareStatement(
    "SELECT * FROM table WHERE id = ?"
);
stmt.setInt(1, userId);
```

### Network Access Control

```java
@Override
public boolean requiresNetwork() {
    return true; // Mark tools that need network access
}
```

The sandbox can be configured to block network access for tools that don't require it.

## Testing Tools

### Unit Test Example

```java
@SpringBootTest
class FileReadToolTest {

    @Autowired
    private FileReadTool fileReadTool;

    @Test
    void testReadExistingFile() throws Exception {
        // Create test file
        Path testFile = Paths.get(FileTool.WORKSPACE_ROOT, "test.txt");
        Files.writeString(testFile, "Hello, World!");

        // Execute tool
        Map<String, Object> params = Map.of("path", "test.txt");
        Map<String, Object> result = fileReadTool.execute(params);

        // Verify result
        assertTrue((Boolean) result.get("success"));
        assertEquals("Hello, World!", result.get("content"));

        // Cleanup
        Files.delete(testFile);
    }

    @Test
    void testReadNonExistentFile() throws Exception {
        Map<String, Object> params = Map.of("path", "nonexistent.txt");
        Map<String, Object> result = fileReadTool.execute(params);

        assertFalse((Boolean) result.get("success"));
        assertTrue(result.get("error").toString().contains("not found"));
    }

    @Test
    void testPathTraversalAttack() {
        Map<String, Object> params = Map.of("path", "../../etc/passwd");

        assertThrows(SecurityException.class, () -> {
            fileReadTool.execute(params);
        });
    }
}
```

## Performance Optimization

### Caching Tool Metadata

Tool descriptions are cached by ToolRegistry for fast prompt generation.

### Async Execution (Future Enhancement)

For long-running tools, consider async execution:

```java
@Async
public CompletableFuture<Map<String, Object>> executeAsync(Map<String, Object> params) {
    return CompletableFuture.supplyAsync(() -> execute(params));
}
```

## Logging Best Practices

```java
// Use emoji prefixes for easy scanning
log.info("📖 Reading file: {}", filePath);
log.info("✅ Successfully read file: {} ({} bytes)", filePath, content.length());
log.warn("⚠️ File not found: {}", resolvedPath);
log.error("❌ Error reading file: {}", filePath, e);
log.error("🚨 Security violation reading file: {}", filePath, e);
```

## Tool Categories

Currently implemented tools by category:

### File Operations (7 tools)
- file_read, file_write, file_list, file_find_by_name, file_find_content, file_replace_string

### Browser Automation (9 tools)
- browser_navigate, browser_view, browser_click, browser_input, browser_scroll_up, browser_scroll_down, browser_press_key, browser_refresh

### Shell Execution (1 tool)
- shell_exec

### Communication (2 tools)
- message_notify_user, message_ask_user

### Web Search (1 tool)
- search_web (SerpAPI/Google API/browser guidance)

### Utilities (3 tools)
- print, todo, search_tools

### Data Visualization (1 tool)
- visualize_data (matplotlib/seaborn/plotly guidance)

## Future Tool Ideas

- **Database Tools**: query_postgres, query_sqlite
- **API Tools**: http_get, http_post, graphql_query
- **Media Tools**: image_resize, pdf_merge, video_thumbnail
- **Analysis Tools**: analyze_csv, json_to_csv, xml_parse
- **ML Tools**: classify_text, extract_entities, summarize
- **Code Tools**: format_code, run_tests, lint_python

## Best Practices Summary

1. **Always extend base classes** (FileTool, BrowserTool) when appropriate
2. **Validate all inputs** before processing
3. **Return consistent response format** (success/error with metadata)
4. **Log with emoji prefixes** for easy debugging
5. **Handle errors gracefully** with helpful messages
6. **Mark network requirements** accurately
7. **Write comprehensive tests** including security tests
8. **Document Python signatures** clearly
9. **Keep tools focused** on single responsibility
10. **Consider security first** (sandboxing, validation, escaping)
