package connecta.post.client;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class SocialClientFallbackFactory implements FallbackFactory<SocialClient> {

    private static final Logger log = LoggerFactory.getLogger(SocialClientFallbackFactory.class);

    @Override
    public SocialClient create(Throwable cause) {
        return new SocialClient() {
            @Override
            public FollowStateDto isFollowing(java.util.UUID userId) {
                log.warn("Social Service unavailable; treating viewer as not following. cause={}", cause.toString());
                return new FollowStateDto(false, false);
            }

            @Override
            public FollowingIdsDto followingIds() {
                log.warn("Social Service unavailable; returning empty following ids. cause={}", cause.toString());
                return new FollowingIdsDto(List.of());
            }
        };
    }
}
