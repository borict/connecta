package connecta.message.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class DirectPairId implements Serializable {

    private UUID userAId;
    private UUID userBId;

    public DirectPairId() {
    }

    public DirectPairId(UUID userAId, UUID userBId) {
        this.userAId = userAId;
        this.userBId = userBId;
    }

    public UUID getUserAId() {
        return userAId;
    }

    public UUID getUserBId() {
        return userBId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DirectPairId other)) {
            return false;
        }
        return Objects.equals(userAId, other.userAId)
                && Objects.equals(userBId, other.userBId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userAId, userBId);
    }
}
