package connecta.message.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation extends AuditEntity {

    @Id
    private UUID id;

    protected Conversation() {
    }

    public Conversation(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
