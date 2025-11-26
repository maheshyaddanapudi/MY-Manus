package ai.mymanus.tool.impl;

import ai.mymanus.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Tool for searching external MCP (Model Context Protocol) tool servers
 *
 * This tool discovers tools from EXTERNAL MCP servers, not the 22 core infrastructure tools.
 * Core infrastructure tools (file operations, browser, shell, etc.) are ALWAYS pre-registered
 * with the LLM and do not need to be searched.
 *
 * MCP Tool Discovery:
 * - Connects to configured MCP tool servers
 * - Searches for tools matching the capability query
 * - Returns tool definitions that can be dynamically loaded
 * - Returns empty list if no MCP servers are configured
 *
 * Example MCP servers:
 * - Email MCP Server: send_email, read_inbox, search_emails
 * - Calendar MCP Server: create_event, list_events, update_event
 * - Database MCP Server: query_postgres, query_mysql, run_migration
 *
 * Configuration:
 * mcp:
 *   servers:
 *     - url: http://email-mcp-server:8080
 *       name: Email Tools
 *     - url: http://calendar-mcp-server:8080
 *       name: Calendar Tools
 */
@Slf4j
@Component
public class SearchToolsTool implements Tool {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mcp.servers:}")
    private List<String> mcpServerUrls;

    @Value("${mcp.enabled:false}")
    private boolean mcpEnabled;

    @Override
    public String getName() {
        return "search_tools";
    }

    @Override
    public String getDescription() {
        return """
            Search for additional tools from external MCP (Model Context Protocol) servers.
            Use this when you need capabilities beyond the core infrastructure tools.

            The 22 core infrastructure tools (file ops, browser, shell, etc.) are always available
            and do NOT need to be searched. This tool discovers EXTERNAL tools from MCP servers.

            Parameters:
            - query: Natural language description of needed capability (e.g., "send email", "book flight")
            - top_k: Number of tools to return (default: 5, max: 10)

            Returns: List of available MCP tools with their descriptions and usage

            Examples:
            search_tools(query="send email", top_k=3)
            # Returns: send_email, read_inbox, search_emails from Email MCP Server

            search_tools(query="flight booking API")
            # Returns: search_flights, book_flight, cancel_booking from Travel MCP Server

            search_tools(query="database operations")
            # Returns: query_postgres, run_migration, backup_db from Database MCP Server

            Note: Returns empty list if no MCP servers are configured.
            Core infrastructure tools are always available without searching.
            """;
    }

    @Override
    public String getPythonSignature() {
        return "search_tools(query: str, top_k: int = 5) -> dict";
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        int topK = params.containsKey("top_k") ?
            Math.min(((Number) params.get("top_k")).intValue(), 10) : 5;

        log.info("🔍 Searching MCP servers for tools: query='{}', top_k={}", query, topK);

        // Check if MCP is enabled and servers are configured
        if (!mcpEnabled || mcpServerUrls == null || mcpServerUrls.isEmpty()) {
            log.info("No MCP servers configured - returning empty list");
            return Map.of(
                "success", true,
                "query", query,
                "tools_found", 0,
                "mcp_servers_queried", 0,
                "tools", Collections.emptyList(),
                "message", "No MCP servers configured. Configure MCP servers in application.yml to discover external tools."
            );
        }

        // Search all configured MCP servers
        List<Map<String, String>> allDiscoveredTools = new ArrayList<>();
        int serversQueried = 0;

        for (String serverUrl : mcpServerUrls) {
            try {
                List<Map<String, String>> serverTools = queryMCPServer(serverUrl, query, topK);
                allDiscoveredTools.addAll(serverTools);
                serversQueried++;
                log.debug("Found {} tools from MCP server: {}", serverTools.size(), serverUrl);
            } catch (Exception e) {
                log.warn("Failed to query MCP server {}: {}", serverUrl, e.getMessage());
            }
        }

        // Sort by relevance and limit to top-k
        List<Map<String, String>> topTools = allDiscoveredTools.stream()
            .limit(topK)
            .toList();

        log.info("✅ Found {} MCP tools from {} servers", topTools.size(), serversQueried);

        return Map.of(
            "success", true,
            "query", query,
            "tools_found", topTools.size(),
            "mcp_servers_queried", serversQueried,
            "tools", topTools
        );
    }

    /**
     * Query a single MCP server for tools matching the query
     *
     * MCP Protocol expects:
     * POST /tools/search
     * {
     *   "query": "send email",
     *   "top_k": 5
     * }
     *
     * Response:
     * {
     *   "tools": [
     *     {
     *       "name": "send_email",
     *       "description": "Send an email message",
     *       "signature": "send_email(to: str, subject: str, body: str) -> dict",
     *       "server": "email-mcp-server"
     *     }
     *   ]
     * }
     */
    private List<Map<String, String>> queryMCPServer(String serverUrl, String query, int topK) {
        try {
            String url = serverUrl + "/tools/search";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("top_k", topK);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> tools =
                    (List<Map<String, String>>) response.getBody().get("tools");

                if (tools != null) {
                    // Add server URL to each tool for tracking
                    for (Map<String, String> tool : tools) {
                        tool.putIfAbsent("mcp_server", serverUrl);
                    }
                    return tools;
                }
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to query MCP server {}: {}", serverUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean requiresNetwork() {
        return true; // Requires network to connect to MCP servers
    }
}
