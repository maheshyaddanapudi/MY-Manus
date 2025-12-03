package ai.mymanus.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * Entity for Spring AI's chat memory table.
 * Schema matches Spring AI's JdbcChatMemoryRepository expectations.
 */
@Entity
@Table(name = "spring_ai_chat_memory", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_conversation_timestamp", columnList = "conversation_id, timestamp")
})
@IdClass(SpringAIChatMemoryId.class)
public class SpringAIChatMemory {

    @Id
    @Column(name = "conversation_id", nullable = false, length = 255)
    private String conversationId;

    @Id
    @Column(name = "timestamp", nullable = false)
    private Timestamp timestamp;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
