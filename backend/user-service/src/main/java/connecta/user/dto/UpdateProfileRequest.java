package connecta.user.dto;

import jakarta.validation.constraints.Size;
import connecta.user.domain.Gender;

public record UpdateProfileRequest(
        @Size(max = 100)
        String displayName,

        @Size(max = 100)
        String bio,

        @Size(max = 100)
        String location,

        Gender gender,

        Boolean isPrivate
) {
}
