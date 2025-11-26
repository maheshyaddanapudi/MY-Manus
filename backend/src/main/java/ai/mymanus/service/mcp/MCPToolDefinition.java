package ai.mymanus.service.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a tool definition from an MCP (Model Context Protocol) client
 *
 * This is the metadata about an external tool that can be discovered
 * and dynamically loaded when the agent needs it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MCPToolDefinition {

    /**
     * Tool name (e.g., "send_email", "create_calendar_event")
     */
    private String name;

    /**
     * Human-readable description of what the tool does
     */
    private String description;

    /**
     * Python function signature
     * Example: "send_email(to: str, subject: str, body: str) -> dict"
     */
    private String signature;

    /**
     * Which MCP client provides this tool
     * Example: "email-client", "calendar-client", "database-client"
     */
    private String sourceClient;

    /**
     * Additional metadata (optional)
     */
    private String metadata;
}
