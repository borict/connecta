package connecta.post.messaging;

public interface PostEventPublisher extends AutoCloseable {

    void publishPostLiked(PostLikedEvent event);

    void publishPostCommented(PostCommentedEvent event);

    @Override
    default void close() {
    }
}
