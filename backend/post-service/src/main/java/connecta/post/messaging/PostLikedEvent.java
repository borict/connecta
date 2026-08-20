package connecta.post.messaging;

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

    public static PostLikedEvent of(UUID postId, UUID postAuthorId, UUID actorId) {
        return new PostLikedEvent(TYPE, Instant.now(), postId, postAuthorId, actorId);
    }
}
