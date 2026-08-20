package connecta.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateCommentRequest", example = """
        {
          "content": "Nice post!"
        }
        """)
public record CreateCommentRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(example = "Nice post!")
        String content
) {
}
