package ai.mymanus.entity;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Composite primary key for SpringAIChatMemory.
 * Uses conversation_id and timestamp as the composite key.
 */
public class SpringAIChatMemoryId implements Serializable {

    private String conversationId;
    private Timestamp timestamp;

    public SpringAIChatMemoryId() {
    }

    public SpringAIChatMemoryId(String conversationId, Timestamp timestamp) {
        this.conversationId = conversationId;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpringAIChatMemoryId that = (SpringAIChatMemoryId) o;
        return Objects.equals(conversationId, that.conversationId) &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(conversationId, timestamp);
    }
}
