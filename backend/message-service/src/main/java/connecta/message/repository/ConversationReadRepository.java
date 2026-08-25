package connecta.message.repository;

import connecta.message.domain.ConversationRead;
import connecta.message.domain.ConversationReadId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationReadRepository extends JpaRepository<ConversationRead, ConversationReadId> {

    Optional<ConversationRead> findByConversationIdAndUserId(UUID conversationId, UUID userId);
}
