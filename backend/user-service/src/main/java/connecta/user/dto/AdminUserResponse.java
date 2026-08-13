package connecta.user.dto;

import java.time.Instant;
import java.util.UUID;
import connecta.user.domain.Role;
import connecta.user.domain.User;

public record AdminUserResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        Role role,
        boolean isActive,
        boolean isBanned,
        boolean isPrivate,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.isBanned(),
                user.isPrivate(),
                user.getCreatedAt()
        );
    }
}
