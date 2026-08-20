package connecta.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreatePostRequest", example = """
        {
          "content": "Hello Connecta!"
        }
        """)
public record CreatePostRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(example = "Hello Connecta!")
        String content
) {
}
