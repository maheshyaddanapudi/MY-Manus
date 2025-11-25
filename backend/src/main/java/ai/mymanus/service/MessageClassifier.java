package ai.mymanus.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Classifies user messages to determine handling strategy
 * Uses LLM to classify messages as TASK, QUERY, or ADJUSTMENT
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageClassifier {

    private final AnthropicService anthropicService;
    private final TodoMdWatcher todoMdWatcher;
    private final MeterRegistry meterRegistry;

    private Counter taskMessages;
    private Counter queryMessages;
    private Counter adjustmentMessages;

    @jakarta.annotation.PostConstruct
    public void initMetrics() {
        taskMessages = Counter.builder("messages.classified.task")
            .description("Messages classified as TASK")
            .register(meterRegistry);

        queryMessages = Counter.builder("messages.classified.query")
            .description("Messages classified as QUERY")
            .register(meterRegistry);

        adjustmentMessages = Counter.builder("messages.classified.adjustment")
            .description("Messages classified as ADJUSTMENT")
            .register(meterRegistry);
    }

    /**
     * Classify a user message
     */
    public MessageType classifyMessage(String sessionId, String userMessage) {
        // Check if there's an active plan
        boolean hasPlan = todoMdWatcher.getCurrentTodo(sessionId).isPresent();

        if (!hasPlan) {
            // No active plan, this is a new task
            log.debug("No active plan, classifying as TASK");
            taskMessages.increment();
            return MessageType.TASK;
        }

        // Get plan context
        String planContext = todoMdWatcher.getCurrentTodo(sessionId)
            .map(plan -> String.format("Plan: %s (%d tasks)", plan.getTitle(), plan.getTasks().size()))
            .orElse("No active plan");

        // Use LLM to classify the message
        String classificationPrompt = String.format("""
            You are a message classifier. Classify the following user message into ONE of these categories:

            1. TASK - User wants to start a NEW task or plan (different from current plan)
            2. QUERY - User has a QUICK QUESTION that doesn't affect the current plan
            3. ADJUSTMENT - User wants to MODIFY, PAUSE, or STOP the current plan

            Current context:
            %s

            User message: "%s"

            Rules:
            - Questions about facts, weather, prices, etc. are QUERY
            - Requests to cancel, change, adjust the plan are ADJUSTMENT
            - Requests to start something completely different are TASK
            - "What is X?" or "How much is Y?" are QUERY
            - "Change the hotel" or "Add a step" are ADJUSTMENT
            - "Start a new project" or "Plan something else" are TASK

            Respond with ONLY ONE WORD: TASK, QUERY, or ADJUSTMENT
            """,
            planContext,
            userMessage
        );

        try {
            String response = anthropicService.sendSimpleMessage(classificationPrompt).trim().toUpperCase();
            MessageType type = MessageType.valueOf(response);

            // Increment metrics
            switch (type) {
                case TASK -> taskMessages.increment();
                case QUERY -> queryMessages.increment();
                case ADJUSTMENT -> adjustmentMessages.increment();
            }

            log.info("Classified message as: {} for session: {}", type, sessionId);
            return type;

        } catch (Exception e) {
            log.warn("Failed to classify message, defaulting to QUERY: {}", e.getMessage());
            queryMessages.increment();
            return MessageType.QUERY;
        }
    }

    public enum MessageType {
        TASK,        // New task/plan
        QUERY,       // Quick question (non-interrupting)
        ADJUSTMENT   // Modify current plan
    }
}
