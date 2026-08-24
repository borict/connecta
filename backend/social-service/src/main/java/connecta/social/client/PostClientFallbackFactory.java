package connecta.social.client;

import connecta.social.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class PostClientFallbackFactory implements FallbackFactory<PostClient> {

    private static final Logger log = LoggerFactory.getLogger(PostClientFallbackFactory.class);

    @Override
    public PostClient create(Throwable cause) {
        return (ids, page, size) -> {
            log.warn("Post Service unavailable; returning empty feed. cause={}", cause.toString());
            int safeSize = size <= 0 ? 20 : Math.min(size, 50);
            int safePage = Math.max(page, 0);
            return PageResponse.empty(safePage, safeSize);
        };
    }
}
