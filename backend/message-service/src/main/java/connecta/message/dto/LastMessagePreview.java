package connecta.message.dto;

import java.time.Instant;
import java.util.UUID;

public record LastMessagePreview(
        UUID id,
        UUID senderId,
        String content,
        Instant createdAt
) {
}
