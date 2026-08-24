package connecta.notification.messaging;

import java.time.Instant;
import java.util.UUID;

public record PostLikedEvent(
        String eventType,
        Instant occurredAt,
        UUID postId,
        UUID postAuthorId,
        UUID actorId
) {
    public static final String TYPE = "POST_LIKED";
}
