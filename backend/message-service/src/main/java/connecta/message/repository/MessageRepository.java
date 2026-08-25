package connecta.message.repository;

import connecta.message.domain.Message;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    long countByConversationIdAndSenderIdNotAndCreatedAtAfter(
            UUID conversationId,
            UUID senderId,
            Instant createdAt
    );

    long countByConversationIdAndSenderIdNot(UUID conversationId, UUID senderId);
}
