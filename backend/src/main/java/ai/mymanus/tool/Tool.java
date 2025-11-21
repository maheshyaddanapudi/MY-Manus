package ai.mymanus.tool;

import java.util.Map;

/**
 * Interface for all tools that agents can use.
 * Tools are exposed as Python functions in the sandbox environment.
 */
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
