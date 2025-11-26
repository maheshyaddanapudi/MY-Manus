package ai.mymanus.service.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Base implementation for MCP clients that connect to external MCP servers
 *
 * This handles the common logic for:
 * - Connecting to an MCP server
 * - Querying the server's tool catalog
 * - Executing tools via the server
 * - Caching tool definitions
 *
 * Subclasses just need to provide the server URL and configuration.
 */
@Slf4j
public abstract class BaseMCPClient implements MCPClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private List<MCPToolDefinition> cachedTools = null;
    private long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Get the URL of the MCP server this client connects to
     */
    protected abstract String getServerUrl();

    /**
     * Get the name of this MCP client
     */
    protected abstract String getClientName();

    @Override
    public String getName() {
        return getClientName();
    }

    @Override
    public List<MCPToolDefinition> getAvailableTools() {
        // Check cache first
        if (cachedTools != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS) {
            log.debug("Returning cached tools for MCP client: {}", getName());
            return cachedTools;
        }

        // Query the MCP server for its tool catalog
        try {
            String url = getServerUrl() + "/tools/list";

            log.info("Querying MCP server for tools: {}", url);

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> serverTools =
                    (List<Map<String, String>>) response.getBody().get("tools");

                if (serverTools != null) {
                    // Convert server response to MCPToolDefinition objects
                    List<MCPToolDefinition> tools = new ArrayList<>();
                    for (Map<String, String> toolData : serverTools) {
                        MCPToolDefinition tool = new MCPToolDefinition(
                            toolData.get("name"),
                            toolData.get("description"),
                            toolData.get("signature"),
                            getName(), // source client
                            toolData.get("metadata")
                        );
                        tools.add(tool);
                    }

                    // Update cache
                    cachedTools = tools;
                    cacheTimestamp = System.currentTimeMillis();

                    log.info("✅ Retrieved {} tools from MCP server: {}", tools.size(), getName());
                    return tools;
                }
            }

            log.warn("Failed to get tools from MCP server: {} (status: {})",
                getName(), response.getStatusCode());
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to query MCP server {}: {}", getName(), e.getMessage());
            // Return cached tools if available, even if stale
            return cachedTools != null ? cachedTools : Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> executeTool(String toolName, Map<String, Object> parameters) throws Exception {
        String url = getServerUrl() + "/tools/execute";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("tool", toolName);
        requestBody.put("parameters", parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Executing tool {} on MCP server {}", toolName, getName());

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
                return result;
            }

            throw new Exception("MCP server returned status: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to execute tool {} on MCP server {}: {}",
                toolName, getName(), e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            String url = getServerUrl() + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("MCP server {} is not available: {}", getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Invalidate the tool cache
     * Useful when you know the MCP server's tools have changed
     */
    protected void invalidateCache() {
        cachedTools = null;
        cacheTimestamp = 0;
        log.info("Invalidated tool cache for MCP client: {}", getName());
    }
}
