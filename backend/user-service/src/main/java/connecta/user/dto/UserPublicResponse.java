package connecta.user.dto;

import java.util.UUID;
import connecta.user.domain.Gender;
import connecta.user.domain.User;

public record UserPublicResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String profilePictureUrl,
        String location,
        Gender gender,
        boolean isPrivate
) {
    public static UserPublicResponse from(User user) {
        return new UserPublicResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getProfilePictureUrl(),
                user.getLocation(),
                user.getGender(),
                user.isPrivate()
        );
    }
}
