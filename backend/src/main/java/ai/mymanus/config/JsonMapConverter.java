package ai.mymanus.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA AttributeConverter for Map<String, Object> to JSON string conversion
 * Works with both H2 and PostgreSQL
 * Handles both direct JSON strings and JSON-as-string (quoted) formats
 */
@Converter
@Slf4j
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting Map to JSON string", e);
            return "{}";
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // First, try to parse as direct JSON
            Map<String, Object> map = objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
            return new HashMap<>(map);
        } catch (JsonProcessingException e) {
            // If that fails, the data might be stored as a JSON string (quoted)
            // Try to parse it as a string first, then parse the result as JSON
            try {
                // Remove surrounding quotes if present and unescape
                String unquoted = dbData;
                if (dbData.startsWith("\"") && dbData.endsWith("\"")) {
                    // Parse as JSON string to handle escaping
                    unquoted = objectMapper.readValue(dbData, String.class);
                }
                
                // Now try to parse the unquoted string as JSON
                Map<String, Object> map = objectMapper.readValue(unquoted, new TypeReference<Map<String, Object>>() {});
                return new HashMap<>(map);
            } catch (JsonProcessingException ex) {
                log.error("Error converting JSON string to Map. Data: {}", dbData, ex);
                return new HashMap<>();
            }
        }
    }
}
