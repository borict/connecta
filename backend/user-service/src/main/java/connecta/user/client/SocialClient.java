package connecta.user.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "social-service",
        fallbackFactory = SocialClientFallbackFactory.class
)
public interface SocialClient {

    @GetMapping("/api/social/{userId}/is-following")
    FollowStateDto isFollowing(@PathVariable("userId") UUID userId);
}
