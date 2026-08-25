package connecta.message.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ConversationParticipantId implements Serializable {

    private UUID conversationId;
    private UUID userId;

    public ConversationParticipantId() {
    }

    public ConversationParticipantId(UUID conversationId, UUID userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConversationParticipantId other)) {
            return false;
        }
        return Objects.equals(conversationId, other.conversationId)
                && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, userId);
    }
}
