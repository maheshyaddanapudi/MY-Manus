package ai.mymanus.service;

import ai.mymanus.model.AgentState;
import ai.mymanus.model.Event;
import ai.mymanus.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service for managing the event stream.
 * Implements Manus AI's event stream pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final AgentStateService stateService;

    /**
     * Append a user message to the event stream
     */
    @Transactional
    public Event appendUserMessage(String sessionId, String message, int iteration) {
        AgentState state = stateService.getSession(sessionId);

        Event event = Event.builder()
                .agentState(state)
                .type(Event.EventType.USER_MESSAGE)
                .iteration(iteration)
                .sequence(getNextSequence(state.getId(), iteration))
                .content(message)
                .success(true)
                .build();

        Event saved = eventRepository.save(event);
        log.debug("Appended USER_MESSAGE to event stream: session={}, iteration={}", sessionId, iteration);
        return saved;
    }

    /**
     * Append an agent thought to the event stream
     */
    @Transactional
    public Event appendAgentThought(String sessionId, String thought, int iteration) {
        AgentState state = stateService.getSession(sessionId);

        Event event = Event.builder()
                .agentState(state)
                .type(Event.EventType.AGENT_THOUGHT)
                .iteration(iteration)
                .sequence(getNextSequence(state.getId(), iteration))
                .content(thought)
                .success(true)
                .build();

        Event saved = eventRepository.save(event);
        log.debug("Appended AGENT_THOUGHT to event stream: session={}, iteration={}", sessionId, iteration);
        return saved;
    }

    /**
     * Append an agent action to the event stream
     */
    @Transactional
    public Event appendAgentAction(String sessionId, String actionType, String actionContent,
                                     Map<String, Object> actionData, int iteration) {
        AgentState state = stateService.getSession(sessionId);

        Event event = Event.builder()
                .agentState(state)
                .type(Event.EventType.AGENT_ACTION)
                .iteration(iteration)
                .sequence(getNextSequence(state.getId(), iteration))
                .content(actionContent)
                .data(actionData)
                .success(true)
                .build();

        Event saved = eventRepository.save(event);
        log.info("Appended AGENT_ACTION to event stream: session={}, iteration={}, action={}",
                sessionId, iteration, actionType);
        return saved;
    }

    /**
     * Append an observation to the event stream
     */
    @Transactional
    public Event appendObservation(String sessionId, String observation, Map<String, Object> observationData,
                                     boolean success, String error, long durationMs, int iteration) {
        AgentState state = stateService.getSession(sessionId);

        Event event = Event.builder()
                .agentState(state)
                .type(Event.EventType.OBSERVATION)
                .iteration(iteration)
                .sequence(getNextSequence(state.getId(), iteration))
                .content(observation)
                .data(observationData)
                .success(success)
                .error(error)
                .durationMs(durationMs)
                .build();

        Event saved = eventRepository.save(event);
        log.info("Appended OBSERVATION to event stream: session={}, iteration={}, success={}, duration={}ms",
                sessionId, iteration, success, durationMs);
        return saved;
    }

    /**
     * Append an agent response to the event stream
     */
    @Transactional
    public Event appendAgentResponse(String sessionId, String response, int iteration) {
        AgentState state = stateService.getSession(sessionId);

        Event event = Event.builder()
                .agentState(state)
                .type(Event.EventType.AGENT_RESPONSE)
                .iteration(iteration)
                .sequence(getNextSequence(state.getId(), iteration))
                .content(response)
                .success(true)
                .build();

        Event saved = eventRepository.save(event);
        log.debug("Appended AGENT_RESPONSE to event stream: session={}, iteration={}", sessionId, iteration);
        return saved;
    }

    /**
     * Append a system message to the event stream
     */
    @Transactional
    public Event appendSystem(String sessionId, String message, int iteration) {
        AgentState state = stateService.getSession(sessionId);

        Event event = Event.builder()
                .agentState(state)
                .type(Event.EventType.SYSTEM)
                .iteration(iteration)
                .sequence(getNextSequence(state.getId(), iteration))
                .content(message)
                .success(true)
                .build();

        Event saved = eventRepository.save(event);
        log.debug("Appended SYSTEM to event stream: session={}, iteration={}", sessionId, iteration);
        return saved;
    }

    /**
     * Append an error to the event stream
     */
    @Transactional
    public Event appendError(String sessionId, String errorMessage, Map<String, Object> errorData, int iteration) {
        AgentState state = stateService.getSession(sessionId);

        Event event = Event.builder()
                .agentState(state)
                .type(Event.EventType.ERROR)
                .iteration(iteration)
                .sequence(getNextSequence(state.getId(), iteration))
                .content(errorMessage)
                .data(errorData)
                .success(false)
                .error(errorMessage)
                .build();

        Event saved = eventRepository.save(event);
        log.warn("Appended ERROR to event stream: session={}, iteration={}, error={}",
                sessionId, iteration, errorMessage);
        return saved;
    }

    /**
     * Get all events for a session
     */
    public List<Event> getEventStream(String sessionId) {
        AgentState state = stateService.getSession(sessionId);
        return eventRepository.findByAgentStateIdOrderByIterationAscSequenceAsc(state.getId());
    }

    /**
     * Get events for a specific iteration
     */
    public List<Event> getEventsForIteration(String sessionId, int iteration) {
        AgentState state = stateService.getSession(sessionId);
        return eventRepository.findByAgentStateIdAndIterationOrderBySequenceAsc(state.getId(), iteration);
    }

    /**
     * Clear event stream for a session
     */
    @Transactional
    public void clearEventStream(String sessionId) {
        AgentState state = stateService.getSession(sessionId);
        eventRepository.deleteByAgentStateId(state.getId());
        log.info("Cleared event stream for session: {}", sessionId);
    }

    /**
     * Get the next sequence number for an iteration
     */
    private int getNextSequence(java.util.UUID agentStateId, int iteration) {
        List<Event> events = eventRepository.findByAgentStateIdAndIterationOrderBySequenceAsc(agentStateId, iteration);
        if (events.isEmpty()) {
            return 1;
        }
        return events.get(events.size() - 1).getSequence() + 1;
    }

    /**
     * Build context string from event stream for LLM prompt
     */
    public String buildEventStreamContext(String sessionId) {
        List<Event> events = getEventStream(sessionId);

        StringBuilder context = new StringBuilder();
        context.append("# Event Stream\n\n");

        for (Event event : events) {
            context.append(String.format("[Iteration %d] ", event.getIteration()));

            switch (event.getType()) {
                case USER_MESSAGE:
                    context.append("User: ").append(event.getContent()).append("\n\n");
                    break;
                case AGENT_THOUGHT:
                    context.append("Thought: ").append(event.getContent()).append("\n\n");
                    break;
                case AGENT_ACTION:
                    context.append("Action: ").append(event.getContent()).append("\n\n");
                    break;
                case OBSERVATION:
                    context.append("Observation: ");
                    if (event.getSuccess()) {
                        context.append(event.getContent());
                    } else {
                        context.append("ERROR: ").append(event.getError());
                    }
                    context.append("\n\n");
                    break;
                case AGENT_RESPONSE:
                    context.append("Response: ").append(event.getContent()).append("\n\n");
                    break;
                case SYSTEM:
                    context.append("System: ").append(event.getContent()).append("\n\n");
                    break;
                case ERROR:
                    context.append("Error: ").append(event.getError()).append("\n\n");
                    break;
            }
        }

        return context.toString();
    }

    /**
     * Get the last observation from the event stream
     * Used for Option A architecture to pass observation to next iteration
     */
    public String getLastObservation(String sessionId) {
        List<Event> events = getEventStream(sessionId);
        
        // Find the most recent OBSERVATION event
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            if (event.getType() == Event.EventType.OBSERVATION) {
                if (event.getSuccess()) {
                    return event.getContent();
                } else {
                    return "ERROR: " + event.getError();
                }
            }
        }
        
        return "No previous observation found";
    }
}
