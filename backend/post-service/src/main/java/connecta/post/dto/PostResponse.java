package connecta.post.dto;

import connecta.post.domain.Post;
import java.time.Instant;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        String content,
        String imageUrl,
        long likeCount,
        long commentCount,
        Instant createdAt
) {
    public static PostResponse from(Post post) {
        return from(post, 0L, 0L);
    }

    public static PostResponse from(Post post, long likeCount, long commentCount) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getContent(),
                post.getImageUrl(),
                likeCount,
                commentCount,
                post.getCreatedAt()
        );
    }
}
