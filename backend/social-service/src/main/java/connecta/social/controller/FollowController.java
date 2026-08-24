package connecta.social.controller;

import connecta.social.dto.FollowResponse;
import connecta.social.dto.PageResponse;
import connecta.social.service.FollowService;
import connecta.social.service.FollowService.FollowResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social")
@Tag(name = "Social")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @Operation(summary = "Follow a user", description = "Public profile → ACCEPTED. Private profile → PENDING. Idempotent.")
    @PostMapping("/{userId}")
    public ResponseEntity<FollowResponse> follow(@PathVariable UUID userId) {
        FollowResult result = followService.follow(userId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.follow());
    }

    @Operation(summary = "Unfollow a user", description = "Also cancels a pending request. Idempotent.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfollow(@PathVariable UUID userId) {
        followService.unfollow(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List incoming follow requests", description = "PENDING requests where the current user is the followee.")
    @GetMapping("/me/requests")
    public PageResponse<FollowResponse> incomingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return followService.incomingRequests(page, size);
    }

    @Operation(summary = "Accept a follow request", description = "Only the private profile owner. userId is the follower. Idempotent if already ACCEPTED.")
    @PostMapping("/{userId}/accept")
    public FollowResponse accept(@PathVariable UUID userId) {
        return followService.accept(userId);
    }

    @Operation(summary = "Reject a follow request", description = "Deletes the PENDING row. Idempotent if no request exists.")
    @PostMapping("/{userId}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID userId) {
        followService.reject(userId);
        return ResponseEntity.noContent().build();
    }
}
