package connecta.message.service;

import connecta.message.domain.Conversation;
import connecta.message.domain.ConversationRead;
import connecta.message.domain.DirectPair;
import connecta.message.domain.DirectPairId;
import connecta.message.domain.DirectPairIds;
import connecta.message.domain.Message;
import connecta.message.dto.CreateMessageRequest;
import connecta.message.dto.MessageResponse;
import connecta.message.dto.PageResponse;
import connecta.message.repository.ConversationReadRepository;
import connecta.message.repository.ConversationRepository;
import connecta.message.repository.DirectPairRepository;
import connecta.message.repository.MessageRepository;
import connecta.message.security.AuthenticatedUser;
import connecta.message.security.SecurityUtils;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {

    static final int MAX_CONTENT_LENGTH = 2000;

    private final DirectPairRepository directPairRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationReadRepository readRepository;

    public MessageService(
            DirectPairRepository directPairRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ConversationReadRepository readRepository
    ) {
        this.directPairRepository = directPairRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.readRepository = readRepository;
    }

    @Transactional
    public MessageResponse send(UUID otherUserId, CreateMessageRequest request) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        UUID conversationId = requireConversationId(currentUser.id(), otherUserId);
        String content = normalizeContent(request == null ? null : request.content());

        Message saved = messageRepository.save(
                new Message(UUID.randomUUID(), conversationId, currentUser.id(), content)
        );
        touchConversation(conversationId, currentUser.id());
        return MessageResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> list(UUID otherUserId, int page, int size) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        UUID conversationId = requireConversationId(currentUser.id(), otherUserId);
        return PageResponse.from(
                messageRepository.findByConversationIdOrderByCreatedAtDesc(
                        conversationId,
                        ConversationService.pageRequest(page, size)
                ).map(MessageResponse::from)
        );
    }

    @Transactional
    public void markRead(UUID otherUserId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        UUID conversationId = requireConversationId(currentUser.id(), otherUserId);
        Instant now = Instant.now();
        ConversationRead read = readRepository
                .findByConversationIdAndUserId(conversationId, currentUser.id())
                .orElse(null);
        if (read == null) {
            readRepository.save(new ConversationRead(conversationId, currentUser.id(), now));
            return;
        }
        read.markRead(now);
        readRepository.save(read);
    }

    private UUID requireConversationId(UUID currentUserId, UUID otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot start a conversation with yourself");
        }
        DirectPairIds pair = DirectPairIds.of(currentUserId, otherUserId);
        return directPairRepository.findById(new DirectPairId(pair.userAId(), pair.userBId()))
                .map(DirectPair::getConversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private void touchConversation(UUID conversationId, UUID updatedBy) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        conversation.setUpdatedBy(updatedBy);
        conversationRepository.save(conversation);
    }

    static String normalizeContent(String raw) {
        String content = raw == null ? "" : raw.trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must not be blank");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Content must be at most " + MAX_CONTENT_LENGTH + " characters"
            );
        }
        return content;
    }
}
