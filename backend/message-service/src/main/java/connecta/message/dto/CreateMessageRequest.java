package connecta.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateMessageRequest", example = """
        {
          "content": "Hey!"
        }
        """)
public record CreateMessageRequest(
        @NotBlank
        @Size(max = 2000)
        @Schema(example = "Hey!")
        String content
) {
}
