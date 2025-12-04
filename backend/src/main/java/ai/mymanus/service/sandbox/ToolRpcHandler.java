package ai.mymanus.service.sandbox;

import ai.mymanus.model.ToolExecution;
import ai.mymanus.service.AgentStateService;
import ai.mymanus.tool.Tool;
import ai.mymanus.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Shared RPC handler for tool execution requests from Python sandbox.
 * Used by both HostPythonExecutor and PythonSandboxExecutor.
 * 
 * Handles the bidirectional RPC protocol:
 * Python → __TOOL_REQUEST__{json}__END__ → Java
 * Java → __TOOL_RESPONSE__{json}__END__ → Python
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRpcHandler {

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final AgentStateService stateService;

    /**
     * Handle a tool request line from Python stdout.
     * Parses the request, executes the tool, and returns the response.
     * 
     * @param line The stdout line containing __TOOL_REQUEST__
     * @return The tool response to send to Python stdin (__TOOL_RESPONSE__{json}__END__)
     */
    public String handleToolRequest(String sessionId, String line) {
        try {
            // Extract JSON from __TOOL_REQUEST__{json}__END__
            String requestJson = extractJson(line, "__TOOL_REQUEST__");
            if (requestJson == null) {
                log.error("Failed to extract JSON from tool request: {}", line);
                return buildErrorResponse("unknown", "Invalid tool request format");
            }

            // Parse request
            ToolRequest request = objectMapper.readValue(requestJson, ToolRequest.class);
            log.debug("🔧 Tool request: {} (id: {})", request.tool, request.id);

            // Get tool from registry
            Tool tool = toolRegistry.getTool(request.tool).orElse(null);
            if (tool == null) {
                log.error("Tool not found: {}", request.tool);
                return buildErrorResponse(request.id, "Tool not found: " + request.tool);
            }

            // Execute tool
            long startTime = System.currentTimeMillis();
            Map<String, Object> result = tool.execute(request.params);
            long duration = System.currentTimeMillis() - startTime;

            log.info("✅ Tool {} executed in {}ms", request.tool, duration);

            // Record tool execution in database for UI display
            try {
                ToolExecution.ExecutionStatus status = (result != null && Boolean.TRUE.equals(result.get("success"))) 
                    ? ToolExecution.ExecutionStatus.SUCCESS 
                    : ToolExecution.ExecutionStatus.FAILED;
                
                stateService.recordToolExecution(sessionId, request.tool, request.params, result, status, (int) duration);
                log.debug("📝 Tool execution recorded in database");
            } catch (Exception e) {
                log.warn("⚠️ Failed to record tool execution (non-fatal): {}", e.getMessage());
            }

            // Build response
            ToolResponse response = new ToolResponse();
            response.id = request.id;
            response.result = result;
            response.error = null;

            String responseJson = objectMapper.writeValueAsString(response);
            return "__TOOL_RESPONSE__" + responseJson + "__END__";

        } catch (Exception e) {
            log.error("Error handling tool request", e);
            return buildErrorResponse("unknown", "Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Extract JSON from a marker-delimited string
     */
    private String extractJson(String line, String marker) {
        try {
            int startIdx = line.indexOf(marker);
            if (startIdx == -1) {
                return null;
            }
            startIdx += marker.length();

            int endIdx = line.indexOf("__END__", startIdx);
            if (endIdx == -1) {
                return null;
            }

            return line.substring(startIdx, endIdx);
        } catch (Exception e) {
            log.error("Error extracting JSON from line", e);
            return null;
        }
    }

    /**
     * Build an error response
     */
    private String buildErrorResponse(String requestId, String errorMessage) {
        try {
            ToolResponse response = new ToolResponse();
            response.id = requestId;
            response.result = null;
            response.error = errorMessage;

            String responseJson = objectMapper.writeValueAsString(response);
            return "__TOOL_RESPONSE__" + responseJson + "__END__";
        } catch (Exception e) {
            log.error("Error building error response", e);
            return "__TOOL_RESPONSE__{\"id\":\"" + requestId + "\",\"error\":\"Internal error\"}__END__";
        }
    }

    /**
     * Tool request structure (from Python)
     */
    public static class ToolRequest {
        public String id;
        public String tool;
        public Map<String, Object> params;
    }

    /**
     * Tool response structure (to Python)
     */
    public static class ToolResponse {
        public String id;
        public Map<String, Object> result;
        public String error;
    }
}
