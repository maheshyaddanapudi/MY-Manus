package ai.mymanus.repository;

import ai.mymanus.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Event stream
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Find all events for an agent state, ordered by iteration and sequence
     */
    List<Event> findByAgentStateIdOrderByIterationAscSequenceAsc(UUID agentStateId);

    /**
     * Find events for a specific iteration
     */
    List<Event> findByAgentStateIdAndIterationOrderBySequenceAsc(UUID agentStateId, Integer iteration);

    /**
     * Delete all events for an agent state
     */
    void deleteByAgentStateId(UUID agentStateId);
}
