package connecta.message.dto;

import java.util.UUID;

public record ChatSendRequest(
        UUID conversationId,
        String content
) {
}
