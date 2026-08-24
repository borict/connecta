package connecta.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notifications_source_message_id",
                columnNames = "source_message_id"
        )
)
public class Notification extends AuditEntity {

    @Id
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 50)
    private ResourceType resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "source_message_id", length = 128)
    private String sourceMessageId;

    protected Notification() {
    }

    public Notification(
            UUID id,
            UUID recipientId,
            UUID actorId,
            NotificationType type,
            ResourceType resourceType,
            UUID resourceId,
            String message,
            String sourceMessageId
    ) {
        this.id = id;
        this.recipientId = recipientId;
        this.actorId = actorId;
        this.type = type;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.message = message;
        this.read = false;
        this.sourceMessageId = sourceMessageId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public NotificationType getType() {
        return type;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public void markRead() {
        this.read = true;
    }
}
