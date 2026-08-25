package connecta.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "direct_pairs")
@IdClass(DirectPairId.class)
public class DirectPair {

    @Id
    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Id
    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @Column(name = "conversation_id", nullable = false, unique = true)
    private UUID conversationId;

    protected DirectPair() {
    }

    public DirectPair(UUID firstUserId, UUID secondUserId, UUID conversationId) {
        DirectPairIds pair = DirectPairIds.of(firstUserId, secondUserId);
        this.userAId = pair.userAId();
        this.userBId = pair.userBId();
        this.conversationId = conversationId;
    }

    public UUID getUserAId() {
        return userAId;
    }

    public UUID getUserBId() {
        return userBId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public DirectPairId getId() {
        return new DirectPairId(userAId, userBId);
    }
}
