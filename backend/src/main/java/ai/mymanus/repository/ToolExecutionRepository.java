package ai.mymanus.repository;

import ai.mymanus.model.ToolExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ToolExecutionRepository extends JpaRepository<ToolExecution, Long> {
    List<ToolExecution> findByAgentStateIdOrderByTimestampAsc(UUID stateId);
}
