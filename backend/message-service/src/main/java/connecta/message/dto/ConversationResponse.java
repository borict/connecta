package connecta.message.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        UUID otherUserId,
        String otherUsername,
        String otherDisplayName,
        String otherProfilePictureUrl,
        LastMessagePreview lastMessage,
        Instant lastMessageAt,
        long unreadCount
) {
}
