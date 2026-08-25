package connecta.message.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageEntitiesTest {

    @Test
    void conversationAndMessageHoldAssignedIds() {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Conversation conversation = new Conversation(conversationId);
        Message message = new Message(messageId, conversationId, senderId, "Hello");

        assertThat(conversation.getId()).isEqualTo(conversationId);
        assertThat(message.getId()).isEqualTo(messageId);
        assertThat(message.getConversationId()).isEqualTo(conversationId);
        assertThat(message.getSenderId()).isEqualTo(senderId);
        assertThat(message.getContent()).isEqualTo("Hello");
    }

    @Test
    void conversationRead_markRead_updatesTimestamp() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant first = Instant.parse("2026-08-25T10:00:00Z");
        Instant second = Instant.parse("2026-08-25T11:00:00Z");

        ConversationRead read = new ConversationRead(conversationId, userId, first);
        read.markRead(second);

        assertThat(read.getConversationId()).isEqualTo(conversationId);
        assertThat(read.getUserId()).isEqualTo(userId);
        assertThat(read.getLastReadAt()).isEqualTo(second);
    }

    @Test
    void participant_bindsUserToConversation() {
        UUID conversationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ConversationParticipant participant = new ConversationParticipant(conversationId, userId);

        assertThat(participant.getConversationId()).isEqualTo(conversationId);
        assertThat(participant.getUserId()).isEqualTo(userId);
    }
}
