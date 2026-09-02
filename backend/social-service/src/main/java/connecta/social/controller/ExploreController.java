package connecta.social.controller;

import connecta.social.client.FeedPostDto;
import connecta.social.config.OpenApiConfig;
import connecta.social.dto.PageResponse;
import connecta.social.service.ExploreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/explore")
@Tag(name = "Explore", description = "Discovery feed from public authors the viewer does not follow")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ExploreController {

    private final ExploreService exploreService;

    public ExploreController(ExploreService exploreService) {
        this.exploreService = exploreService;
    }

    @Operation(
            summary = "Explore feed",
            description = "Posts from public users the current user does not ACCEPTED-follow, newest first. Empty page if User or Post Service is down."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping
    public PageResponse<FeedPostDto> explore(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return exploreService.explore(page, size);
    }
}
