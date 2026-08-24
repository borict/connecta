package connecta.social.dto;

import java.time.Instant;
import java.util.UUID;

public record FollowUserResponse(
        UUID userId,
        String username,
        String displayName,
        String profilePictureUrl,
        Instant followedAt
) {
}
