package connecta.notification.messaging;

public interface NotificationEventConsumer extends AutoCloseable {

    void start();
}
