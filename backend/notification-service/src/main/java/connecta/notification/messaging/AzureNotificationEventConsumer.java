package connecta.notification.messaging;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureNotificationEventConsumer implements NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AzureNotificationEventConsumer.class);

    private final ServiceBusProcessorClient processor;

    public AzureNotificationEventConsumer(ServiceBusProcessorClient processor) {
        this.processor = processor;
        log.info("Azure Service Bus consumer enabled for notifications");
    }

    @Override
    public void start() {
        processor.start();
    }

    @Override
    public void close() {
        processor.close();
    }
}
