package connecta.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotificationTypeTest {

    @Test
    void includesPhase4TypesAndReservedMessage() {
        assertThat(NotificationType.values()).containsExactly(
                NotificationType.LIKE,
                NotificationType.COMMENT,
                NotificationType.FOLLOW,
                NotificationType.MESSAGE
        );
    }

    @Test
    void resourceTypesMatchSchemaCheck() {
        assertThat(ResourceType.values()).containsExactly(
                ResourceType.POST,
                ResourceType.COMMENT,
                ResourceType.USER,
                ResourceType.CONVERSATION
        );
    }
}
