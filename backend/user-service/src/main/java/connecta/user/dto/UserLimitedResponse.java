package connecta.user.dto;

import java.util.UUID;
import connecta.user.domain.User;

public record UserLimitedResponse(
        UUID id,
        String username,
        String displayName,
        String profilePictureUrl,
        boolean isPrivate
) {
    public static UserLimitedResponse from(User user) {
        return new UserLimitedResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                true
        );
    }
}
