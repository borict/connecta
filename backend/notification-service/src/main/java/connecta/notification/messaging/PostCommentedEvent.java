package connecta.notification.messaging;

import java.time.Instant;
import java.util.UUID;

public record PostCommentedEvent(
        String eventType,
        Instant occurredAt,
        UUID postId,
        UUID postAuthorId,
        UUID actorId,
        UUID commentId,
        String content
) {
    public static final String TYPE = "POST_COMMENTED";
}
