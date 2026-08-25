package connecta.message.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpMessageEventPublisher implements MessageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpMessageEventPublisher.class);

    public NoOpMessageEventPublisher() {
        log.info("Azure Service Bus publisher disabled (no connection string); message events are no-op");
    }

    @Override
    public void publishMessageSent(MessageSentEvent event) {
        log.debug("Skipping MESSAGE_SENT event for conversation {}", event.conversationId());
    }
}
