package ai.mymanus.multiagent;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for individual agent in multi-agent system
 * Supports custom LLM per agent with fallback to primary LLM
 */
@Data
@Builder
public class AgentConfig {
    /**
     * Unique agent ID
     */
    private String agentId;

    /**
     * Role of this agent
     */
    private AgentRole role;

    /**
     * Custom LLM model for this agent (optional)
     * If null, falls back to primary LLM configured in application
     */
    private String llmModel;

    /**
     * Custom system prompt for this agent (optional)
     * If null, uses role-based default prompt
     */
    private String systemPrompt;

    /**
     * Temperature for LLM (0.0 to 1.0)
     * If null, uses default (0.7)
     */
    private Double temperature;

    /**
     * Max tokens for LLM response
     * If null, uses default
     */
    private Integer maxTokens;

    /**
     * Whether this agent can delegate to other agents
     */
    @Builder.Default
    private boolean canDelegate = false;

    /**
     * Maximum iterations for this agent
     */
    @Builder.Default
    private int maxIterations = 10;

    /**
     * Get effective LLM model (with fallback to primary)
     */
    public String getEffectiveLlmModel(String primaryLlm) {
        return llmModel != null ? llmModel : primaryLlm;
    }

    /**
     * Get effective system prompt (with fallback to role-based)
     */
    public String getEffectiveSystemPrompt() {
        if (systemPrompt != null) {
            return systemPrompt;
        }

        // Generate role-based prompt
        return generateRolePrompt();
    }

    private String generateRolePrompt() {
        return switch (role) {
            case COORDINATOR -> """
                You are a Coordinator Agent responsible for breaking down complex tasks into smaller sub-tasks.
                Your job is to:
                1. Analyze the user's request
                2. Break it into logical sub-tasks
                3. Delegate sub-tasks to specialist agents
                4. Aggregate results from all agents
                5. Provide a final comprehensive answer
                """;

            case PLANNER -> """
                You are a Planner Agent responsible for creating detailed execution plans.
                Your job is to:
                1. Analyze the task requirements
                2. Create a step-by-step plan
                3. Identify dependencies between steps
                4. Provide clear actionable steps
                """;

            case EXECUTOR -> """
                You are an Executor Agent responsible for performing technical tasks.
                Your job is to:
                1. Write and execute code to complete tasks
                2. Use available tools effectively
                3. Handle errors and retry when appropriate
                4. Provide clear execution results
                """;

            case VERIFIER -> """
                You are a Verifier Agent responsible for validating results.
                Your job is to:
                1. Check outputs for correctness
                2. Verify code executes without errors
                3. Validate against requirements
                4. Suggest improvements if needed
                """;

            case RESEARCHER -> """
                You are a Researcher Agent responsible for gathering information.
                Your job is to:
                1. Search for relevant information
                2. Read and analyze documentation
                3. Compile research findings
                4. Provide well-sourced answers
                """;

            case CODE_REVIEWER -> """
                You are a Code Reviewer Agent responsible for code quality.
                Your job is to:
                1. Review code for bugs and issues
                2. Check for best practices
                3. Suggest improvements
                4. Ensure code is maintainable
                """;
        };
    }
}
