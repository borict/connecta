package connecta.post.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpPostEventPublisher implements PostEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpPostEventPublisher.class);

    public NoOpPostEventPublisher() {
        log.info("Azure Service Bus publisher disabled (no connection string); post events are no-op");
    }

    @Override
    public void publishPostLiked(PostLikedEvent event) {
        log.debug("Skipping POST_LIKED event for post {}", event.postId());
    }

    @Override
    public void publishPostCommented(PostCommentedEvent event) {
        log.debug("Skipping POST_COMMENTED event for post {}", event.postId());
    }
}
