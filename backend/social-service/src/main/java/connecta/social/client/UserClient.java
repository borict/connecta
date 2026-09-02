package connecta.social.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "user-service",
        fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {

    @GetMapping("/api/users/batch")
    List<UserSummaryDto> batchUsers(@RequestParam("ids") String ids);

    @GetMapping("/api/users/public-ids")
    PublicIdsDto publicIds();
}
