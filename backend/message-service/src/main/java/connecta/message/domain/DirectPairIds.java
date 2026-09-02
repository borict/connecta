package connecta.message.domain;

import java.util.UUID;

public record DirectPairIds(UUID userAId, UUID userBId) {

    public DirectPairIds {
        if (userAId == null || userBId == null) {
            throw new IllegalArgumentException("Both user ids are required");
        }
        if (userAId.equals(userBId)) {
            throw new IllegalArgumentException("A direct pair requires two different users");
        }
        if (compareUnsigned(userAId, userBId) > 0) {
            throw new IllegalArgumentException("userAId must be less than userBId");
        }
    }

    public static DirectPairIds of(UUID first, UUID second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("Both user ids are required");
        }
        if (first.equals(second)) {
            throw new IllegalArgumentException("A direct pair requires two different users");
        }
        return compareUnsigned(first, second) < 0
                ? new DirectPairIds(first, second)
                : new DirectPairIds(second, first);
    }

    /**
     * Same order as PostgreSQL {@code uuid < uuid} (unsigned 128-bit).
     * {@link UUID#compareTo} is signed and can disagree with the DB check constraint.
     */
    static int compareUnsigned(UUID left, UUID right) {
        int high = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        if (high != 0) {
            return high;
        }
        return Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }
}
