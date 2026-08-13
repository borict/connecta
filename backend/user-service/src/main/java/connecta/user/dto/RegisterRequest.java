package connecta.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import connecta.user.domain.Gender;
import connecta.user.validation.MinAge;

/**
 * Registration payload. Role is never accepted from the client — new users always get USER.
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Size(max = 100)
        String displayName,

        @NotNull
        @Past
        @MinAge(15)
        LocalDate dateOfBirth,

        @Size(max = 100)
        String bio,

        @Size(max = 100)
        String location,

        Gender gender,

        Boolean isPrivate
) {
}
