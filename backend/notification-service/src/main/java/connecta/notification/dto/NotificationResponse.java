package connecta.notification.dto;

import connecta.notification.domain.Notification;
import connecta.notification.domain.NotificationType;
import connecta.notification.domain.ResourceType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID actorId,
        NotificationType type,
        ResourceType resourceType,
        UUID resourceId,
        String message,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getActorId(),
                notification.getType(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
