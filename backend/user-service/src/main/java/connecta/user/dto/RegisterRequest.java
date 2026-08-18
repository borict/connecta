package connecta.user.dto;

import connecta.user.domain.Gender;
import connecta.user.validation.MinAge;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Registration payload. Role is never accepted from the client — new users always get USER.
 */
@Schema(name = "RegisterRequest", example = """
        {
          "username": "tamara",
          "email": "tamara@example.com",
          "password": "Secret123!",
          "displayName": "Tamara",
          "dateOfBirth": "2000-05-10",
          "bio": "cao",
          "location": "Novi Sad",
          "gender": "FEMALE",
          "isPrivate": false
        }
        """)
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Schema(example = "tamara")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(example = "tamara@example.com")
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        @Schema(example = "Secret123!")
        String password,

        @NotBlank
        @Size(max = 100)
        @Schema(example = "Tamara")
        String displayName,

        @NotNull
        @Past
        @MinAge(15)
        @Schema(example = "2000-05-10", type = "string", format = "date")
        LocalDate dateOfBirth,

        @Size(max = 100)
        @Schema(example = "cao")
        String bio,

        @Size(max = 100)
        @Schema(example = "Novi Sad")
        String location,

        @Schema(example = "FEMALE")
        Gender gender,

        @Schema(example = "false")
        Boolean isPrivate
) {
}
