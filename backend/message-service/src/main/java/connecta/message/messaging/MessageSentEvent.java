package connecta.message.messaging;

import java.time.Instant;
import java.util.UUID;

public record MessageSentEvent(
        String eventType,
        Instant occurredAt,
        UUID conversationId,
        UUID messageId,
        UUID senderId,
        UUID recipientId
) {
    public static final String TYPE = "MESSAGE_SENT";

    public static MessageSentEvent of(UUID conversationId, UUID messageId, UUID senderId, UUID recipientId) {
        return new MessageSentEvent(TYPE, Instant.now(), conversationId, messageId, senderId, recipientId);
    }
}
