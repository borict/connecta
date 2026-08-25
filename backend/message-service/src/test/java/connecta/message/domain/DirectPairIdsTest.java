package connecta.message.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DirectPairIdsTest {

    @Test
    void of_sortsIdsSoUserAIsLessThanUserB() {
        UUID lower = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higher = UUID.fromString("00000000-0000-0000-0000-000000000002");

        DirectPairIds forward = DirectPairIds.of(lower, higher);
        DirectPairIds reverse = DirectPairIds.of(higher, lower);

        assertThat(forward.userAId()).isEqualTo(lower);
        assertThat(forward.userBId()).isEqualTo(higher);
        assertThat(reverse).isEqualTo(forward);
        assertThat(reverse.userAId().compareTo(reverse.userBId())).isNegative();
    }

    @Test
    void of_sameUsersInEitherOrder_shareTheSameIdentity() {
        UUID first = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID second = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        DirectPairId forwardKey = new DirectPairId(
                DirectPairIds.of(first, second).userAId(),
                DirectPairIds.of(first, second).userBId()
        );
        DirectPairId reverseKey = new DirectPairId(
                DirectPairIds.of(second, first).userAId(),
                DirectPairIds.of(second, first).userBId()
        );

        assertThat(forwardKey).isEqualTo(reverseKey);
        assertThat(forwardKey.hashCode()).isEqualTo(reverseKey.hashCode());
    }

    @Test
    void of_sameUser_throws() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> DirectPairIds.of(userId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A direct pair requires two different users");
    }

    @Test
    void of_nullUser_throws() {
        assertThatThrownBy(() -> DirectPairIds.of(null, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both user ids are required");
        assertThatThrownBy(() -> DirectPairIds.of(UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Both user ids are required");
    }

    @Test
    void constructor_rejectsUnsortedPair() {
        UUID lower = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higher = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertThatThrownBy(() -> new DirectPairIds(higher, lower))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userAId must be less than userBId");
    }
}
