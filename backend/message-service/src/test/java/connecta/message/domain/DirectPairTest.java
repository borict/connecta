package connecta.message.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DirectPairTest {

    @Test
    void constructor_sortsUserIdsToMatchUniquePairConstraint() {
        UUID lower = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID higher = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID conversationId = UUID.randomUUID();

        DirectPair forward = new DirectPair(lower, higher, conversationId);
        DirectPair reverse = new DirectPair(higher, lower, conversationId);

        assertThat(forward.getUserAId()).isEqualTo(lower);
        assertThat(forward.getUserBId()).isEqualTo(higher);
        assertThat(reverse.getUserAId()).isEqualTo(lower);
        assertThat(reverse.getUserBId()).isEqualTo(higher);
        assertThat(forward.getId()).isEqualTo(reverse.getId());
        assertThat(forward.getConversationId()).isEqualTo(conversationId);
    }

    @Test
    void constructor_selfPair_throws() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> new DirectPair(userId, userId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A direct pair requires two different users");
    }
}
