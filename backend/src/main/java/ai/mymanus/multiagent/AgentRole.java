package ai.mymanus.multiagent;

/**
 * Defines specialized agent roles for multi-agent orchestration
 */
public enum AgentRole {
    /**
     * Coordinator agent - manages task decomposition and delegation
     */
    COORDINATOR("Breaks down complex tasks and delegates to specialist agents"),

    /**
     * Planner agent - creates detailed execution plans
     */
    PLANNER("Creates step-by-step execution plans for tasks"),

    /**
     * Executor agent - performs actual code execution
     */
    EXECUTOR("Executes code and performs technical tasks"),

    /**
     * Verifier agent - validates results and checks correctness
     */
    VERIFIER("Verifies outputs and ensures quality"),

    /**
     * Researcher agent - gathers information and conducts research
     */
    RESEARCHER("Searches for information and conducts research"),

    /**
     * Code Reviewer agent - reviews code for quality and bugs
     */
    CODE_REVIEWER("Reviews code for bugs, best practices, and improvements");

    private final String description;

    AgentRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
