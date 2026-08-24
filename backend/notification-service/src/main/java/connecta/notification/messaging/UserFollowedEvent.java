package connecta.notification.messaging;

import java.time.Instant;
import java.util.UUID;

public record UserFollowedEvent(
        String eventType,
        Instant occurredAt,
        UUID followerId,
        UUID followeeId
) {
    public static final String TYPE = "USER_FOLLOWED";
}
