package ai.mymanus.service;

import ai.mymanus.model.Message;
import ai.mymanus.tool.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Builds prompts for the CodeAct agent.
 * Structures system prompt with tool descriptions and execution rules.
 */
@Service
@RequiredArgsConstructor
public class PromptBuilder {

    private final ToolRegistry toolRegistry;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a helpful AI assistant that solves problems by writing and executing Python code.

            ## How you work:
            1. Think step-by-step about the problem
            2. Write Python code to solve it
            3. Execution happens automatically in a secure sandbox
            4. Review the output and continue if needed
            5. Repeat until the task is complete (max 20 iterations)

            ## Available Tools:
            You have access to these Python functions:

            %s

            ## Execution Environment:
            - Python 3.11 in Ubuntu 22.04
            - Variables persist between code blocks
            - Use print() to show results
            - Files saved in /home/ubuntu/workspace
            - Maximum 30 seconds per execution
            - Network: %s

            ## Code Execution:
            Wrap your Python code in <execute> tags:

            <execute>
            # Your Python code here
            import pandas as pd
            data = pd.DataFrame({'x': [1, 2, 3]})
            print(data.describe())
            </execute>

            ## Current Execution Context:
            Variables in memory: %s

            ## Important Rules:
            - Always use print() to show results to the user
            - Handle errors gracefully with try/except
            - Break complex tasks into smaller steps
            - Save important data to files
            - Use the available tools when appropriate
            - Think before you code - explain your approach

            ## Response Format:
            1. First, explain what you're going to do
            2. Then provide the code in <execute> tags
            3. After execution, analyze the results
            4. Continue or conclude based on the output

            Remember: You're helping a user solve their problem. Be clear, thorough, and precise.
            """;

    /**
     * Build the system prompt with current context
     */
    public String buildSystemPrompt(Map<String, Object> executionContext, boolean networkEnabled) {
        String toolDescriptions = toolRegistry.getToolDescriptions();
        String networkStatus = networkEnabled ? "Enabled" : "Disabled (no external requests)";
        String contextDescription = buildContextDescription(executionContext);

        return String.format(SYSTEM_PROMPT_TEMPLATE,
                toolDescriptions,
                networkStatus,
                contextDescription);
    }

    /**
     * Build a description of the current execution context
     */
    private String buildContextDescription(Map<String, Object> executionContext) {
        if (executionContext == null || executionContext.isEmpty()) {
            return "None (fresh start)";
        }

        StringBuilder sb = new StringBuilder();
        executionContext.forEach((key, value) -> {
            String valueType = value != null ? value.getClass().getSimpleName() : "None";
            sb.append(String.format("- %s: %s\n", key, valueType));
        });

        return sb.toString();
    }

    /**
     * Build conversation history for context
     */
    public String buildConversationHistory(List<Message> messages) {
        StringBuilder history = new StringBuilder();

        for (Message message : messages) {
            String role = switch (message.getRole()) {
                case USER -> "User";
                case ASSISTANT -> "Assistant";
                case SYSTEM -> "System";
            };

            history.append(String.format("## %s:\n%s\n\n", role, message.getContent()));
        }

        return history.toString();
    }

    /**
     * Extract code blocks from LLM response
     */
    public List<String> extractCodeBlocks(String response) {
        List<String> codeBlocks = new java.util.ArrayList<>();
        String[] parts = response.split("<execute>");

        for (int i = 1; i < parts.length; i++) {
            int endIndex = parts[i].indexOf("</execute>");
            if (endIndex != -1) {
                String code = parts[i].substring(0, endIndex).trim();
                codeBlocks.add(code);
            }
        }

        return codeBlocks;
    }

    /**
     * Build observation message from execution result
     */
    public String buildObservation(String stdout, String stderr, boolean success) {
        StringBuilder observation = new StringBuilder();
        observation.append("## Execution Result:\n\n");

        if (success) {
            observation.append("✓ Success\n\n");
        } else {
            observation.append("✗ Error\n\n");
        }

        if (stdout != null && !stdout.trim().isEmpty()) {
            observation.append("### Output:\n```\n");
            observation.append(stdout.trim());
            observation.append("\n```\n\n");
        }

        if (stderr != null && !stderr.trim().isEmpty()) {
            observation.append("### Errors:\n```\n");
            observation.append(stderr.trim());
            observation.append("\n```\n\n");
        }

        if (!success) {
            observation.append("Please fix the error and try again.\n");
        }

        return observation.toString();
    }
}
