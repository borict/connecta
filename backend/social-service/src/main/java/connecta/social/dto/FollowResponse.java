package connecta.social.dto;

import connecta.social.domain.Follow;
import connecta.social.domain.FollowStatus;
import java.time.Instant;
import java.util.UUID;

public record FollowResponse(
        UUID followerId,
        UUID followeeId,
        FollowStatus status,
        Instant createdAt
) {
    public static FollowResponse from(Follow follow) {
        return new FollowResponse(
                follow.getFollowerId(),
                follow.getFolloweeId(),
                follow.getStatus(),
                follow.getCreatedAt()
        );
    }
}
