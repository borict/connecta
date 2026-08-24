package connecta.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void constructorCreatesUnreadNotification() {
        UUID id = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        Notification notification = new Notification(
                id,
                recipientId,
                actorId,
                NotificationType.LIKE,
                ResourceType.POST,
                postId,
                "Someone liked your post",
                "azure-message-1"
        );

        assertThat(notification.getId()).isEqualTo(id);
        assertThat(notification.getRecipientId()).isEqualTo(recipientId);
        assertThat(notification.getActorId()).isEqualTo(actorId);
        assertThat(notification.getType()).isEqualTo(NotificationType.LIKE);
        assertThat(notification.getResourceType()).isEqualTo(ResourceType.POST);
        assertThat(notification.getResourceId()).isEqualTo(postId);
        assertThat(notification.getMessage()).isEqualTo("Someone liked your post");
        assertThat(notification.getSourceMessageId()).isEqualTo("azure-message-1");
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void markRead_setsReadFlag() {
        Notification notification = new Notification(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NotificationType.FOLLOW,
                ResourceType.USER,
                UUID.randomUUID(),
                "Someone started following you",
                null
        );

        notification.markRead();

        assertThat(notification.isRead()).isTrue();
    }
}
