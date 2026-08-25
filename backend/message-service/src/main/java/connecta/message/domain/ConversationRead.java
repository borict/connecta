package connecta.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_reads")
@IdClass(ConversationReadId.class)
public class ConversationRead {

    @Id
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    protected ConversationRead() {
    }

    public ConversationRead(UUID conversationId, UUID userId, Instant lastReadAt) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.lastReadAt = lastReadAt;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }

    public void markRead(Instant at) {
        this.lastReadAt = at;
    }
}
