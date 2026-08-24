package connecta.notification.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import connecta.notification.config.ServiceBusProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NotificationEventConsumers {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumers.class);

    private NotificationEventConsumers() {
    }

    public static NotificationEventConsumer create(
            ServiceBusProperties properties,
            NotificationEventHandler handler
    ) {
        if (!properties.isConfigured()) {
            return new NoOpNotificationEventConsumer();
        }
        try {
            NotificationMessageListener listener = new NotificationMessageListener(handler);
            ServiceBusProcessorClient processor = new ServiceBusClientBuilder()
                    .connectionString(properties.connectionString())
                    .processor()
                    .topicName(properties.topic())
                    .subscriptionName(properties.subscription())
                    .disableAutoComplete()
                    .processMessage(listener::onMessage)
                    .processError(listener::onError)
                    .buildProcessorClient();
            AzureNotificationEventConsumer consumer = new AzureNotificationEventConsumer(processor);
            consumer.start();
            return consumer;
        } catch (RuntimeException ex) {
            log.warn("Could not start Azure Service Bus processor; consumer disabled. cause={}", ex.toString());
            return new NoOpNotificationEventConsumer();
        }
    }
}
