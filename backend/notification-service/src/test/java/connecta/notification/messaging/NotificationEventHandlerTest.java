package connecta.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import connecta.notification.domain.Notification;
import connecta.notification.domain.NotificationType;
import connecta.notification.domain.ResourceType;
import connecta.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private ObjectMapper objectMapper;
    private NotificationEventHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        handler = new NotificationEventHandler(notificationRepository, objectMapper);
    }

    @Test
    void postLiked_createsLikeNotification() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                new PostLikedEvent(PostLikedEvent.TYPE, Instant.parse("2026-08-25T00:00:00Z"), postId, authorId, actorId)
        );

        EventHandleResult result = handler.handle(PostLikedEvent.TYPE, body, "msg-like-1");

        assertThat(result).isEqualTo(EventHandleResult.CREATED);
        Notification saved = capturedNotification();
        assertThat(saved.getRecipientId()).isEqualTo(authorId);
        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getType()).isEqualTo(NotificationType.LIKE);
        assertThat(saved.getResourceType()).isEqualTo(ResourceType.POST);
        assertThat(saved.getResourceId()).isEqualTo(postId);
        assertThat(saved.getMessage()).isEqualTo(NotificationEventHandler.LIKED_MESSAGE);
        assertThat(saved.getSourceMessageId()).isEqualTo("msg-like-1");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void postCommented_createsCommentNotificationForPost() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new PostCommentedEvent(
                PostCommentedEvent.TYPE,
                Instant.parse("2026-08-25T00:00:00Z"),
                postId,
                authorId,
                actorId,
                UUID.randomUUID(),
                "Nice post!"
        ));

        EventHandleResult result = handler.handle(PostCommentedEvent.TYPE, body, "msg-comment-1");

        assertThat(result).isEqualTo(EventHandleResult.CREATED);
        Notification saved = capturedNotification();
        assertThat(saved.getRecipientId()).isEqualTo(authorId);
        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getType()).isEqualTo(NotificationType.COMMENT);
        assertThat(saved.getResourceType()).isEqualTo(ResourceType.POST);
        assertThat(saved.getResourceId()).isEqualTo(postId);
        assertThat(saved.getMessage()).isEqualTo(NotificationEventHandler.COMMENTED_MESSAGE);
        assertThat(saved.getMessage()).doesNotContain("Nice post!");
    }

    @Test
    void userFollowed_createsFollowNotification() throws Exception {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                new UserFollowedEvent(UserFollowedEvent.TYPE, Instant.parse("2026-08-25T00:00:00Z"), followerId, followeeId)
        );

        EventHandleResult result = handler.handle(UserFollowedEvent.TYPE, body, "msg-follow-1");

        assertThat(result).isEqualTo(EventHandleResult.CREATED);
        Notification saved = capturedNotification();
        assertThat(saved.getRecipientId()).isEqualTo(followeeId);
        assertThat(saved.getActorId()).isEqualTo(followerId);
        assertThat(saved.getType()).isEqualTo(NotificationType.FOLLOW);
        assertThat(saved.getResourceType()).isEqualTo(ResourceType.USER);
        assertThat(saved.getResourceId()).isEqualTo(followerId);
        assertThat(saved.getMessage()).isEqualTo(NotificationEventHandler.FOLLOWED_MESSAGE);
    }

    @Test
    void selfLike_isIgnored() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                new PostLikedEvent(PostLikedEvent.TYPE, Instant.now(), UUID.randomUUID(), userId, userId)
        );

        assertThat(handler.handle(PostLikedEvent.TYPE, body, "msg-self-like"))
                .isEqualTo(EventHandleResult.IGNORED);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void selfComment_isIgnored() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new PostCommentedEvent(
                PostCommentedEvent.TYPE,
                Instant.now(),
                UUID.randomUUID(),
                userId,
                userId,
                UUID.randomUUID(),
                "own comment"
        ));

        assertThat(handler.handle(PostCommentedEvent.TYPE, body, "msg-self-comment"))
                .isEqualTo(EventHandleResult.IGNORED);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void selfFollow_isIgnored() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(
                new UserFollowedEvent(UserFollowedEvent.TYPE, Instant.now(), userId, userId)
        );

        assertThat(handler.handle(UserFollowedEvent.TYPE, body, "msg-self-follow"))
                .isEqualTo(EventHandleResult.IGNORED);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void messageSent_createsMessageNotification() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new MessageSentEvent(
                MessageSentEvent.TYPE,
                Instant.parse("2026-08-25T00:00:00Z"),
                conversationId,
                UUID.randomUUID(),
                senderId,
                recipientId
        ));

        EventHandleResult result = handler.handle(MessageSentEvent.TYPE, body, "msg-dm");

        assertThat(result).isEqualTo(EventHandleResult.CREATED);
        Notification saved = capturedNotification();
        assertThat(saved.getRecipientId()).isEqualTo(recipientId);
        assertThat(saved.getActorId()).isEqualTo(senderId);
        assertThat(saved.getType()).isEqualTo(NotificationType.MESSAGE);
        assertThat(saved.getResourceType()).isEqualTo(ResourceType.CONVERSATION);
        assertThat(saved.getResourceId()).isEqualTo(conversationId);
        assertThat(saved.getMessage()).isEqualTo(NotificationEventHandler.MESSAGE_SENT_MESSAGE);
        assertThat(saved.getSourceMessageId()).isEqualTo("msg-dm");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void selfMessage_isIgnored() throws Exception {
        UUID userId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new MessageSentEvent(
                MessageSentEvent.TYPE,
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                userId
        ));

        assertThat(handler.handle(MessageSentEvent.TYPE, body, "msg-self-dm"))
                .isEqualTo(EventHandleResult.IGNORED);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void messageSent_missingRequiredFields_isInvalid() {
        String body = """
                {"eventType":"MESSAGE_SENT","occurredAt":"2026-08-25T00:00:00Z","conversationId":"%s"}
                """.formatted(UUID.randomUUID());

        assertThat(handler.handle(MessageSentEvent.TYPE, body, "msg-incomplete-dm"))
                .isEqualTo(EventHandleResult.INVALID);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void unknownType_isIgnored() {
        String body = """
                {"eventType":"SOMETHING_ELSE","occurredAt":"2026-08-25T00:00:00Z"}
                """;

        assertThat(handler.handle("SOMETHING_ELSE", body, "msg-unknown"))
                .isEqualTo(EventHandleResult.IGNORED);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void duplicateSourceMessageId_isIgnored() throws Exception {
        UUID authorId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new PostLikedEvent(
                PostLikedEvent.TYPE,
                Instant.now(),
                UUID.randomUUID(),
                authorId,
                UUID.randomUUID()
        ));
        when(notificationRepository.existsBySourceMessageId("msg-dup")).thenReturn(true);

        assertThat(handler.handle(PostLikedEvent.TYPE, body, "msg-dup"))
                .isEqualTo(EventHandleResult.IGNORED);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void uniqueConstraintRace_isIgnored() throws Exception {
        UUID authorId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new PostLikedEvent(
                PostLikedEvent.TYPE,
                Instant.now(),
                UUID.randomUUID(),
                authorId,
                UUID.randomUUID()
        ));
        when(notificationRepository.existsBySourceMessageId("msg-race")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("uk_notifications_source_message_id"));

        assertThat(handler.handle(PostLikedEvent.TYPE, body, "msg-race"))
                .isEqualTo(EventHandleResult.IGNORED);
    }

    @Test
    void invalidJson_isInvalid() {
        assertThat(handler.handle(PostLikedEvent.TYPE, "{not-json", "msg-bad"))
                .isEqualTo(EventHandleResult.INVALID);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void missingRequiredFields_isInvalid() {
        String body = """
                {"eventType":"POST_LIKED","occurredAt":"2026-08-25T00:00:00Z","postId":"%s"}
                """.formatted(UUID.randomUUID());

        assertThat(handler.handle(PostLikedEvent.TYPE, body, "msg-incomplete"))
                .isEqualTo(EventHandleResult.INVALID);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void blankBody_isInvalid() {
        assertThat(handler.handle(PostLikedEvent.TYPE, "  ", "msg-empty"))
                .isEqualTo(EventHandleResult.INVALID);
    }

    @Test
    void typeFromBodyWhenSubjectMissing() throws Exception {
        UUID postId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(new PostLikedEvent(
                PostLikedEvent.TYPE,
                Instant.now(),
                postId,
                authorId,
                UUID.randomUUID()
        ));

        assertThat(handler.handle("  ", body, "msg-no-subject"))
                .isEqualTo(EventHandleResult.CREATED);
        assertThat(capturedNotification().getResourceId()).isEqualTo(postId);
    }

    private Notification capturedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }
}
