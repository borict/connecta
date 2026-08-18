package connecta.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        @Schema(example = "admin")
        String usernameOrEmail,

        @NotBlank
        @Schema(example = "Admin123!")
        String password
) {
}
