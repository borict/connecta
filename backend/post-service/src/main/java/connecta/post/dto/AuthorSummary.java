package connecta.post.dto;

import java.util.UUID;

public record AuthorSummary(
        UUID id,
        String username,
        String displayName,
        String profilePictureUrl
) {
    public static AuthorSummary fallback(UUID id) {
        return new AuthorSummary(id, null, null, null);
    }
}
