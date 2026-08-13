package connecta.user.dto;

import java.util.UUID;
import connecta.user.domain.User;

public record UserSummaryResponse(
        UUID id,
        String username,
        String displayName,
        String profilePictureUrl,
        boolean isPrivate
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                user.isPrivate()
        );
    }
}
