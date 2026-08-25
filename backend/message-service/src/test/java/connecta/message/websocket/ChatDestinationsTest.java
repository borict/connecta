package connecta.message.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatDestinationsTest {

    @Test
    void conversationTopic_usesLockedPrefix() {
        UUID conversationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        assertThat(ChatDestinations.conversationTopic(conversationId))
                .isEqualTo("/topic/conversations.aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    @Test
    void conversationIdFromTopic_parsesLockedDestination() {
        UUID conversationId = UUID.randomUUID();

        assertThat(ChatDestinations.conversationIdFromTopic(ChatDestinations.conversationTopic(conversationId)))
                .contains(conversationId);
    }

    @Test
    void conversationIdFromTopic_rejectsOtherDestinations() {
        assertThat(ChatDestinations.conversationIdFromTopic(null)).isEmpty();
        assertThat(ChatDestinations.conversationIdFromTopic("/topic/conversations")).isEmpty();
        assertThat(ChatDestinations.conversationIdFromTopic("/user/queue/notifications")).isEmpty();
        assertThat(ChatDestinations.conversationIdFromTopic("/topic/conversations.not-a-uuid")).isEmpty();
    }
}
