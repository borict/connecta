package connecta.notification.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import connecta.notification.domain.Notification;
import connecta.notification.domain.NotificationType;
import connecta.notification.domain.ResourceType;
import connecta.notification.repository.NotificationRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventHandler {

    static final String LIKED_MESSAGE = "Someone liked your post";
    static final String COMMENTED_MESSAGE = "Someone commented on your post";
    static final String FOLLOWED_MESSAGE = "Someone started following you";

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public NotificationEventHandler(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EventHandleResult handle(String eventType, String body, String sourceMessageId) {
        if (body == null || body.isBlank()) {
            return EventHandleResult.INVALID;
        }

        String type;
        try {
            type = resolveType(eventType, body);
        } catch (JsonProcessingException ex) {
            return EventHandleResult.INVALID;
        }
        if (type == null || type.isBlank()) {
            return EventHandleResult.INVALID;
        }

        return switch (type) {
            case PostLikedEvent.TYPE -> handleLiked(body, sourceMessageId);
            case PostCommentedEvent.TYPE -> handleCommented(body, sourceMessageId);
            case UserFollowedEvent.TYPE -> handleFollowed(body, sourceMessageId);
            default -> {
                log.debug("Ignoring unknown notification event type={}", type);
                yield EventHandleResult.IGNORED;
            }
        };
    }

    private EventHandleResult handleLiked(String body, String sourceMessageId) {
        PostLikedEvent event;
        try {
            event = objectMapper.readValue(body, PostLikedEvent.class);
        } catch (JsonProcessingException ex) {
            return EventHandleResult.INVALID;
        }
        if (event.postId() == null || event.postAuthorId() == null || event.actorId() == null) {
            return EventHandleResult.INVALID;
        }
        if (event.postAuthorId().equals(event.actorId())) {
            return EventHandleResult.IGNORED;
        }
        return persist(new Notification(
                UUID.randomUUID(),
                event.postAuthorId(),
                event.actorId(),
                NotificationType.LIKE,
                ResourceType.POST,
                event.postId(),
                LIKED_MESSAGE,
                normalizeSourceId(sourceMessageId)
        ));
    }

    private EventHandleResult handleCommented(String body, String sourceMessageId) {
        PostCommentedEvent event;
        try {
            event = objectMapper.readValue(body, PostCommentedEvent.class);
        } catch (JsonProcessingException ex) {
            return EventHandleResult.INVALID;
        }
        if (event.postId() == null || event.postAuthorId() == null || event.actorId() == null) {
            return EventHandleResult.INVALID;
        }
        if (event.postAuthorId().equals(event.actorId())) {
            return EventHandleResult.IGNORED;
        }
        return persist(new Notification(
                UUID.randomUUID(),
                event.postAuthorId(),
                event.actorId(),
                NotificationType.COMMENT,
                ResourceType.POST,
                event.postId(),
                COMMENTED_MESSAGE,
                normalizeSourceId(sourceMessageId)
        ));
    }

    private EventHandleResult handleFollowed(String body, String sourceMessageId) {
        UserFollowedEvent event;
        try {
            event = objectMapper.readValue(body, UserFollowedEvent.class);
        } catch (JsonProcessingException ex) {
            return EventHandleResult.INVALID;
        }
        if (event.followerId() == null || event.followeeId() == null) {
            return EventHandleResult.INVALID;
        }
        if (event.followerId().equals(event.followeeId())) {
            return EventHandleResult.IGNORED;
        }
        return persist(new Notification(
                UUID.randomUUID(),
                event.followeeId(),
                event.followerId(),
                NotificationType.FOLLOW,
                ResourceType.USER,
                event.followerId(),
                FOLLOWED_MESSAGE,
                normalizeSourceId(sourceMessageId)
        ));
    }

    private EventHandleResult persist(Notification notification) {
        String sourceId = notification.getSourceMessageId();
        if (sourceId != null && notificationRepository.existsBySourceMessageId(sourceId)) {
            return EventHandleResult.IGNORED;
        }
        try {
            notificationRepository.save(notification);
            return EventHandleResult.CREATED;
        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate notification ignored. sourceMessageId={}", sourceId);
            return EventHandleResult.IGNORED;
        }
    }

    private String resolveType(String eventType, String body) throws JsonProcessingException {
        if (eventType != null && !eventType.isBlank()) {
            return eventType;
        }
        JsonNode node = objectMapper.readTree(body);
        JsonNode typeNode = node.get("eventType");
        if (typeNode == null || typeNode.isNull() || typeNode.asText().isBlank()) {
            return null;
        }
        return typeNode.asText();
    }

    private static String normalizeSourceId(String sourceMessageId) {
        if (sourceMessageId == null || sourceMessageId.isBlank()) {
            return null;
        }
        return sourceMessageId;
    }
}
