package connecta.social.controller;

import connecta.social.client.FeedPostDto;
import connecta.social.dto.PageResponse;
import connecta.social.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
@Tag(name = "Feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @Operation(
            summary = "Home feed",
            description = "Posts from ACCEPTED followees plus the current user, newest first. Empty page if Post Service is down."
    )
    @GetMapping
    public PageResponse<FeedPostDto> feed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return feedService.feed(page, size);
    }
}
