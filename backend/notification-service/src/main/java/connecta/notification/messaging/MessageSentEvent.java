package connecta.notification.messaging;

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
}
