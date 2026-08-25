package connecta.message.websocket;

import java.util.Optional;
import java.util.UUID;

public final class ChatDestinations {

    public static final String APP_SEND = "/chat.send";
    public static final String TOPIC_PREFIX = "/topic/conversations.";

    private ChatDestinations() {
    }

    public static String conversationTopic(UUID conversationId) {
        return TOPIC_PREFIX + conversationId;
    }

    public static Optional<UUID> conversationIdFromTopic(String destination) {
        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            return Optional.empty();
        }
        String raw = destination.substring(TOPIC_PREFIX.length());
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
