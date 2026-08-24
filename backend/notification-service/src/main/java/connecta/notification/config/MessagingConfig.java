package connecta.notification.config;

import connecta.notification.messaging.NotificationEventConsumer;
import connecta.notification.messaging.NotificationEventConsumers;
import connecta.notification.messaging.NotificationEventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    @Bean(destroyMethod = "close")
    NotificationEventConsumer notificationEventConsumer(
            ServiceBusProperties properties,
            NotificationEventHandler handler
    ) {
        return NotificationEventConsumers.create(properties, handler);
    }
}
