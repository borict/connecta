package connecta.post.dto;

import connecta.post.domain.Post;
import java.time.Instant;
import java.util.UUID;

public record PostResponse(
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
    public static PostResponse from(Post post, long likeCount, long commentCount, AuthorSummary author) {
        AuthorSummary safe = author != null ? author : AuthorSummary.fallback(post.getAuthorId());
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                safe.username(),
                safe.displayName(),
                safe.profilePictureUrl(),
                post.getContent(),
                post.getImageUrl(),
                likeCount,
                commentCount,
                post.getCreatedAt()
        );
    }
}
