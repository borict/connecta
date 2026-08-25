package connecta.message.repository;

import connecta.message.domain.Conversation;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            SELECT c FROM Conversation c
            JOIN ConversationParticipant p ON p.conversationId = c.id
            WHERE p.userId = :userId
            ORDER BY c.updatedAt DESC
            """)
    Page<Conversation> findByParticipantUserIdOrderByUpdatedAtDesc(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}
