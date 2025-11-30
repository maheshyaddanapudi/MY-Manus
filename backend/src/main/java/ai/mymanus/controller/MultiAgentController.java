package ai.mymanus.controller;

import ai.mymanus.multiagent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for multi-agent orchestration
 */
@RestController
@RequestMapping("/api/multi-agent")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MultiAgentController {

    private final MultiAgentOrchestrator orchestrator;

    /**
     * Execute task using multi-agent orchestration
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("sessionId");
            String task = (String) request.get("task");
            Boolean sequential = (Boolean) request.getOrDefault("sequential", false);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> agentConfigsRaw = (List<Map<String, Object>>) request.get("agents");

            List<AgentConfig> agentConfigs;

            if (agentConfigsRaw == null || agentConfigsRaw.isEmpty()) {
                // Use default pipeline
                agentConfigs = orchestrator.createDefaultAgentPipeline();
            } else {
                // Parse agent configs
                agentConfigs = agentConfigsRaw.stream()
                        .map(this::parseAgentConfig)
                        .toList();
            }

            Map<String, Object> result = orchestrator.orchestrate(sessionId, task, agentConfigs, sequential);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error in multi-agent execution", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get default agent pipeline
     */
    @GetMapping("/default-pipeline")
    public ResponseEntity<List<Map<String, Object>>> getDefaultPipeline() {
        List<AgentConfig> configs = orchestrator.createDefaultAgentPipeline();

        List<Map<String, Object>> response = configs.stream()
                .map(config -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("agentId", config.getAgentId());
                    map.put("role", config.getRole().name());
                    map.put("description", config.getRole().getDescription());
                    map.put("llmModel", config.getLlmModel() != null ? config.getLlmModel() : "primary");
                    map.put("maxIterations", config.getMaxIterations());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Get available agent roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getAvailableRoles() {
        List<Map<String, Object>> roles = java.util.Arrays.stream(AgentRole.values())
                .map(role -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("name", role.name());
                    map.put("description", role.getDescription());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(roles);
    }

    private AgentConfig parseAgentConfig(Map<String, Object> raw) {
        String agentId = (String) raw.get("agentId");
        String roleName = (String) raw.get("role");
        String llmModel = (String) raw.get("llmModel");
        String systemPrompt = (String) raw.get("systemPrompt");
        Integer maxIterations = raw.get("maxIterations") != null ?
                ((Number) raw.get("maxIterations")).intValue() : 10;

        AgentRole role = AgentRole.valueOf(roleName);

        return AgentConfig.builder()
                .agentId(agentId)
                .role(role)
                .llmModel(llmModel)  // Can be null - will fallback to primary
                .systemPrompt(systemPrompt)
                .maxIterations(maxIterations)
                .build();
    }
}
