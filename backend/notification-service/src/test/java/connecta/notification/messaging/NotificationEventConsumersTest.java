package connecta.notification.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import connecta.notification.config.ServiceBusProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumersTest {

    @Mock
    private NotificationEventHandler handler;

    @Test
    void missingConnectionString_returnsNoOp() {
        ServiceBusProperties properties = new ServiceBusProperties(
                "",
                "connecta-events",
                "notification-service"
        );

        NotificationEventConsumer consumer = NotificationEventConsumers.create(properties, handler);

        assertThat(consumer).isInstanceOf(NoOpNotificationEventConsumer.class);
        assertThatCode(() -> {
            consumer.start();
            consumer.close();
        }).doesNotThrowAnyException();
    }

    @Test
    void invalidConnectionString_returnsNoOp() {
        ServiceBusProperties properties = new ServiceBusProperties(
                "not-a-connection-string",
                "connecta-events",
                "notification-service"
        );

        NotificationEventConsumer consumer = NotificationEventConsumers.create(properties, handler);

        assertThat(consumer).isInstanceOf(NoOpNotificationEventConsumer.class);
    }
}
