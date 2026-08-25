package connecta.message.messaging;

public interface MessageEventPublisher extends AutoCloseable {

    void publishMessageSent(MessageSentEvent event);

    @Override
    default void close() {
    }
}
