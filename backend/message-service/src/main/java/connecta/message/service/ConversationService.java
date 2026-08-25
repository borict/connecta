package connecta.message.service;

import connecta.message.client.UserSummaryDto;
import connecta.message.domain.Conversation;
import connecta.message.domain.ConversationParticipant;
import connecta.message.domain.ConversationRead;
import connecta.message.domain.DirectPair;
import connecta.message.domain.DirectPairId;
import connecta.message.domain.DirectPairIds;
import connecta.message.domain.Message;
import connecta.message.dto.ConversationResponse;
import connecta.message.dto.LastMessagePreview;
import connecta.message.dto.PageResponse;
import connecta.message.repository.ConversationParticipantRepository;
import connecta.message.repository.ConversationReadRepository;
import connecta.message.repository.ConversationRepository;
import connecta.message.repository.DirectPairRepository;
import connecta.message.repository.MessageRepository;
import connecta.message.security.AuthenticatedUser;
import connecta.message.security.SecurityUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConversationService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;

    public record ConversationResult(ConversationResponse conversation, boolean created) {
    }

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectPairRepository directPairRepository;
    private final MessageRepository messageRepository;
    private final ConversationReadRepository readRepository;
    private final UserLookupService userLookup;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            DirectPairRepository directPairRepository,
            MessageRepository messageRepository,
            ConversationReadRepository readRepository,
            UserLookupService userLookup
    ) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.directPairRepository = directPairRepository;
        this.messageRepository = messageRepository;
        this.readRepository = readRepository;
        this.userLookup = userLookup;
    }

    @Transactional
    public ConversationResult getOrCreate(UUID otherUserId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        rejectSelf(currentUser.id(), otherUserId);

        DirectPairId pairId = pairId(currentUser.id(), otherUserId);
        DirectPair existing = directPairRepository.findById(pairId).orElse(null);
        if (existing != null) {
            return new ConversationResult(toResponse(existing.getConversationId(), currentUser.id()), false);
        }

        userLookup.requireActiveUser(otherUserId);

        UUID conversationId = UUID.randomUUID();
        try {
            conversationRepository.save(new Conversation(conversationId));
            participantRepository.save(new ConversationParticipant(conversationId, currentUser.id()));
            participantRepository.save(new ConversationParticipant(conversationId, otherUserId));
            directPairRepository.save(new DirectPair(currentUser.id(), otherUserId, conversationId));
            return new ConversationResult(toResponse(conversationId, currentUser.id()), true);
        } catch (DataIntegrityViolationException ex) {
            DirectPair raced = directPairRepository.findById(pairId)
                    .orElseThrow(() -> ex);
            return new ConversationResult(toResponse(raced.getConversationId(), currentUser.id()), false);
        }
    }

    @Transactional(readOnly = true)
    public ConversationResponse getWithUser(UUID otherUserId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        rejectSelf(currentUser.id(), otherUserId);

        DirectPair existing = directPairRepository.findById(pairId(currentUser.id(), otherUserId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        return toResponse(existing.getConversationId(), currentUser.id());
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> list(int page, int size) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Page<Conversation> result = conversationRepository.findByParticipantUserIdOrderByUpdatedAtDesc(
                currentUser.id(),
                pageRequest(page, size)
        );
        List<UUID> conversationIds = result.getContent().stream()
                .map(Conversation::getId)
                .toList();
        Map<UUID, UUID> otherUserByConversation = otherUserIds(conversationIds, currentUser.id());
        Map<UUID, UserSummaryDto> users = userLookup.summariesByIds(otherUserByConversation.values());

        List<ConversationResponse> content = result.getContent().stream()
                .map(conversation -> toResponse(
                        conversation,
                        currentUser.id(),
                        otherUserByConversation.get(conversation.getId()),
                        users
                ))
                .toList();
        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private ConversationResponse toResponse(UUID conversationId, UUID currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        UUID otherUserId = participantRepository.findByConversationIdAndUserIdNot(conversationId, currentUserId)
                .map(ConversationParticipant::getUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        Map<UUID, UserSummaryDto> users = userLookup.summariesByIds(List.of(otherUserId));
        return toResponse(conversation, currentUserId, otherUserId, users);
    }

    private ConversationResponse toResponse(
            Conversation conversation,
            UUID currentUserId,
            UUID otherUserId,
            Map<UUID, UserSummaryDto> users
    ) {
        if (otherUserId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
        UserSummaryDto other = users.get(otherUserId);
        Message last = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);
        LastMessagePreview preview = last == null
                ? null
                : new LastMessagePreview(last.getId(), last.getSenderId(), last.getContent(), last.getCreatedAt());
        return new ConversationResponse(
                conversation.getId(),
                otherUserId,
                other == null ? null : other.username(),
                other == null ? null : other.displayName(),
                other == null ? null : other.profilePictureUrl(),
                preview,
                last != null ? last.getCreatedAt() : conversation.getCreatedAt(),
                unreadCount(conversation.getId(), currentUserId)
        );
    }

    private long unreadCount(UUID conversationId, UUID currentUserId) {
        ConversationRead read = readRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElse(null);
        if (read == null) {
            return messageRepository.countByConversationIdAndSenderIdNot(conversationId, currentUserId);
        }
        return messageRepository.countByConversationIdAndSenderIdNotAndCreatedAtAfter(
                conversationId,
                currentUserId,
                read.getLastReadAt()
        );
    }

    private Map<UUID, UUID> otherUserIds(List<UUID> conversationIds, UUID currentUserId) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> result = new HashMap<>();
        for (ConversationParticipant participant : participantRepository.findByConversationIdIn(conversationIds)) {
            if (!currentUserId.equals(participant.getUserId())) {
                result.put(participant.getConversationId(), participant.getUserId());
            }
        }
        return result;
    }

    private static DirectPairId pairId(UUID first, UUID second) {
        DirectPairIds pair = DirectPairIds.of(first, second);
        return new DirectPairId(pair.userAId(), pair.userBId());
    }

    private static void rejectSelf(UUID currentUserId, UUID otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot start a conversation with yourself");
        }
    }

    static PageRequest pageRequest(int page, int size) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize);
    }
}
