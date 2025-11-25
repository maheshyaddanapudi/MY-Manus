package ai.mymanus.service;

import ai.mymanus.model.Event;
import ai.mymanus.model.AgentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Session Replay Service
 * Reconstructs agent state at any point in execution history
 * Supports time-travel debugging and step-through replay
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionReplayService {

    private final EventService eventService;
    private final AgentStateService agentStateService;

    /**
     * Get snapshot of session state at specific event
     */
    public Map<String, Object> getStateAtEvent(String sessionId, Long eventId) {
        log.info("📹 Reconstructing state at event {} for session {}", eventId, sessionId);

        List<Event> allEvents = eventService.getEventStream(sessionId);

        // Get events up to and including the target event
        List<Event> eventsUpToTarget = allEvents.stream()
                .filter(e -> e.getId() <= eventId)
                .sorted(Comparator.comparing(Event::getSequence))
                .collect(Collectors.toList());

        return buildStateSnapshot(sessionId, eventsUpToTarget);
    }

    /**
     * Get snapshot at specific iteration
     */
    public Map<String, Object> getStateAtIteration(String sessionId, int iteration) {
        log.info("📹 Reconstructing state at iteration {} for session {}", iteration, sessionId);

        List<Event> allEvents = eventService.getEventStream(sessionId);

        // Get all events from iterations up to target
        List<Event> eventsUpToIteration = allEvents.stream()
                .filter(e -> {
                    Map<String, Object> data = e.getData();
                    if (data != null && data.containsKey("iteration")) {
                        int eventIteration = ((Number) data.get("iteration")).intValue();
                        return eventIteration <= iteration;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Event::getSequence))
                .collect(Collectors.toList());

        return buildStateSnapshot(sessionId, eventsUpToIteration);
    }

    /**
     * Get all replay points (iterations) for session
     */
    public List<Map<String, Object>> getReplayPoints(String sessionId) {
        List<Event> allEvents = eventService.getEventStream(sessionId);

        // Group events by iteration
        Map<Integer, List<Event>> eventsByIteration = new HashMap<>();

        for (Event event : allEvents) {
            Map<String, Object> data = event.getData();
            int iteration = 0;

            if (data != null && data.containsKey("iteration")) {
                iteration = ((Number) data.get("iteration")).intValue();
            }

            eventsByIteration.computeIfAbsent(iteration, k -> new ArrayList<>()).add(event);
        }

        // Create replay points
        List<Map<String, Object>> replayPoints = new ArrayList<>();

        for (Map.Entry<Integer, List<Event>> entry : eventsByIteration.entrySet()) {
            int iteration = entry.getKey();
            List<Event> iterationEvents = entry.getValue();

            Map<String, Object> replayPoint = new HashMap<>();
            replayPoint.put("iteration", iteration);
            replayPoint.put("eventCount", iterationEvents.size());
            replayPoint.put("firstEventId", iterationEvents.get(0).getId());
            replayPoint.put("lastEventId", iterationEvents.get(iterationEvents.size() - 1).getId());

            // Get summary
            String summary = generateIterationSummary(iterationEvents);
            replayPoint.put("summary", summary);

            replayPoints.add(replayPoint);
        }

        // Sort by iteration
        replayPoints.sort(Comparator.comparing(p -> (Integer) p.get("iteration")));

        return replayPoints;
    }

    /**
     * Step forward one event from current position
     */
    public Map<String, Object> stepForward(String sessionId, Long currentEventId) {
        List<Event> allEvents = eventService.getEventStream(sessionId);

        // Find next event
        Optional<Event> nextEvent = allEvents.stream()
                .filter(e -> e.getId() > currentEventId)
                .min(Comparator.comparing(Event::getId));

        if (nextEvent.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "No more events");
            return result;
        }

        return getStateAtEvent(sessionId, nextEvent.get().getId());
    }

    /**
     * Step backward one event from current position
     */
    public Map<String, Object> stepBackward(String sessionId, Long currentEventId) {
        List<Event> allEvents = eventService.getEventStream(sessionId);

        // Find previous event
        Optional<Event> prevEvent = allEvents.stream()
                .filter(e -> e.getId() < currentEventId)
                .max(Comparator.comparing(Event::getId));

        if (prevEvent.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Already at beginning");
            return result;
        }

        return getStateAtEvent(sessionId, prevEvent.get().getId());
    }

    /**
     * Build state snapshot from event history
     */
    private Map<String, Object> buildStateSnapshot(String sessionId, List<Event> events) {
        Map<String, Object> snapshot = new HashMap<>();

        snapshot.put("sessionId", sessionId);
        snapshot.put("eventCount", events.size());

        if (events.isEmpty()) {
            snapshot.put("iteration", 0);
            snapshot.put("events", Collections.emptyList());
            return snapshot;
        }

        // Get last event
        Event lastEvent = events.get(events.size() - 1);
        snapshot.put("currentEventId", lastEvent.getId());
        snapshot.put("currentSequence", lastEvent.getSequence());

        // Reconstruct iteration number
        int currentIteration = 0;
        for (Event event : events) {
            Map<String, Object> data = event.getData();
            if (data != null && data.containsKey("iteration")) {
                currentIteration = Math.max(currentIteration,
                    ((Number) data.get("iteration")).intValue());
            }
        }
        snapshot.put("iteration", currentIteration);

        // Reconstruct Python variables (from OBSERVATION events)
        Map<String, Object> pythonVariables = new HashMap<>();
        for (Event event : events) {
            if (event.getType() == Event.EventType.OBSERVATION) {
                Map<String, Object> data = event.getData();
                if (data != null && data.containsKey("variables")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> vars = (Map<String, Object>) data.get("variables");
                    pythonVariables.putAll(vars);
                }
            }
        }
        snapshot.put("pythonVariables", pythonVariables);

        // Include events
        snapshot.put("events", events);

        // Get last action
        Optional<Event> lastAction = events.stream()
                .filter(e -> e.getType() == Event.EventType.AGENT_ACTION)
                .reduce((first, second) -> second);

        if (lastAction.isPresent()) {
            Map<String, Object> actionData = lastAction.get().getData();
            snapshot.put("lastAction", actionData != null ? actionData.get("pythonCode") : null);
        }

        // Get last observation
        Optional<Event> lastObservation = events.stream()
                .filter(e -> e.getType() == Event.EventType.OBSERVATION)
                .reduce((first, second) -> second);

        if (lastObservation.isPresent()) {
            snapshot.put("lastObservation", lastObservation.get().getData());
        }

        log.info("✅ State snapshot built: iteration={}, events={}", currentIteration, events.size());

        return snapshot;
    }

    /**
     * Generate summary for iteration
     */
    private String generateIterationSummary(List<Event> events) {
        StringBuilder summary = new StringBuilder();

        // Count event types
        long userMessages = events.stream().filter(e -> e.getType() == Event.EventType.USER_MESSAGE).count();
        long agentActions = events.stream().filter(e -> e.getType() == Event.EventType.AGENT_ACTION).count();
        long observations = events.stream().filter(e -> e.getType() == Event.EventType.OBSERVATION).count();
        long errors = events.stream().filter(e -> e.getType() == Event.EventType.ERROR).count();

        if (userMessages > 0) summary.append("User input, ");
        if (agentActions > 0) summary.append(agentActions).append(" action(s), ");
        if (observations > 0) summary.append(observations).append(" observation(s), ");
        if (errors > 0) summary.append(errors).append(" error(s), ");

        String result = summary.toString();
        return result.isEmpty() ? "No events" : result.substring(0, result.length() - 2);
    }
}
