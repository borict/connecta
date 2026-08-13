package connecta.user.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import connecta.user.domain.Gender;
import connecta.user.domain.Role;
import connecta.user.domain.User;

public record UserMeResponse(
        UUID id,
        String username,
        String email,
        String displayName,
        String bio,
        String profilePictureUrl,
        LocalDate dateOfBirth,
        String location,
        Gender gender,
        boolean isPrivate,
        Role role,
        boolean isActive,
        boolean isBanned,
        Instant createdAt
) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getProfilePictureUrl(),
                user.getDateOfBirth(),
                user.getLocation(),
                user.getGender(),
                user.isPrivate(),
                user.getRole(),
                user.isActive(),
                user.isBanned(),
                user.getCreatedAt()
        );
    }
}
