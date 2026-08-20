package connecta.post.messaging;

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

    public static PostCommentedEvent of(
            UUID postId,
            UUID postAuthorId,
            UUID actorId,
            UUID commentId,
            String content
    ) {
        return new PostCommentedEvent(TYPE, Instant.now(), postId, postAuthorId, actorId, commentId, content);
    }
}
