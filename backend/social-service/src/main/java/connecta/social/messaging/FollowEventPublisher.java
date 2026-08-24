package connecta.social.messaging;

public interface FollowEventPublisher extends AutoCloseable {

    void publishUserFollowed(UserFollowedEvent event);

    @Override
    default void close() {
    }
}
