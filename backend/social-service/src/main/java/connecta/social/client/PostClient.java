package connecta.social.client;

import connecta.social.dto.PageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "post-service",
        url = "${connecta.post-service.url:http://localhost:8082}",
        fallbackFactory = PostClientFallbackFactory.class
)
public interface PostClient {

    @GetMapping("/api/posts/by-authors")
    PageResponse<FeedPostDto> listByAuthors(
            @RequestParam("ids") String ids,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    );
}
