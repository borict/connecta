package connecta.social.controller;

import connecta.social.dto.FollowResponse;
import connecta.social.service.FollowService;
import connecta.social.service.FollowService.FollowResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
