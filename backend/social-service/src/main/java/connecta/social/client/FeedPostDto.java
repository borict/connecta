package connecta.social.client;

import java.time.Instant;
import java.util.UUID;

public record FeedPostDto(
        UUID id,
        UUID authorId,
        String authorUsername,
        String authorDisplayName,
        String authorProfilePictureUrl,
        String content,
        String imageUrl,
        long likeCount,
        long commentCount,
        Instant createdAt
) {
}
