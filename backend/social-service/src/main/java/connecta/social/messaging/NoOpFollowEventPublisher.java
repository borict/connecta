package connecta.social.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpFollowEventPublisher implements FollowEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpFollowEventPublisher.class);

    public NoOpFollowEventPublisher() {
        log.info("Azure Service Bus publisher disabled (no connection string); follow events are no-op");
    }

    @Override
    public void publishUserFollowed(UserFollowedEvent event) {
        log.debug("Skipping USER_FOLLOWED event for followee {}", event.followeeId());
    }
}
