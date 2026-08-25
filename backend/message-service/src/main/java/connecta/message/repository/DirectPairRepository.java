package connecta.message.repository;

import connecta.message.domain.DirectPair;
import connecta.message.domain.DirectPairId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectPairRepository extends JpaRepository<DirectPair, DirectPairId> {

    Optional<DirectPair> findByConversationId(UUID conversationId);
}
