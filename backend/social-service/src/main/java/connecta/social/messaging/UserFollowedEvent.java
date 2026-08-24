package connecta.social.messaging;

import java.time.Instant;
import java.util.UUID;

public record UserFollowedEvent(
        String eventType,
        Instant occurredAt,
        UUID followerId,
        UUID followeeId
) {
    public static final String TYPE = "USER_FOLLOWED";

    public static UserFollowedEvent of(UUID followerId, UUID followeeId) {
        return new UserFollowedEvent(TYPE, Instant.now(), followerId, followeeId);
    }
}
