package connecta.post.dto;

import connecta.post.domain.Comment;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID authorId,
        String authorUsername,
        String authorDisplayName,
        String authorProfilePictureUrl,
        String content,
        Instant createdAt
) {
    public static CommentResponse from(Comment comment, AuthorSummary author) {
        AuthorSummary safe = author != null ? author : AuthorSummary.fallback(comment.getAuthorId());
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                safe.username(),
                safe.displayName(),
                safe.profilePictureUrl(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
