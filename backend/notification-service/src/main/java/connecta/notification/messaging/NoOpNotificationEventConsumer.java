package connecta.notification.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpNotificationEventConsumer implements NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NoOpNotificationEventConsumer.class);

    public NoOpNotificationEventConsumer() {
        log.info("Azure Service Bus consumer disabled for notifications");
    }

    @Override
    public void start() {
        log.debug("Skipping Azure Service Bus processor for notifications");
    }

    @Override
    public void close() {
        // no-op
    }
}
