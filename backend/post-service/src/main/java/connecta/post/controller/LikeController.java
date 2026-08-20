package connecta.post.controller;

import connecta.post.dto.LikeCountResponse;
import connecta.post.dto.LikeResponse;
import connecta.post.dto.LikedResponse;
import connecta.post.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @Operation(summary = "Like a post", description = "Idempotent: liking twice keeps a single like.")
    @PostMapping("/{postId}/likes")
    public LikeResponse like(@PathVariable UUID postId) {
        return likeService.like(postId);
    }

    @Operation(summary = "Unlike a post", description = "Idempotent: unliking when not liked is a no-op.")
    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<Void> unlike(@PathVariable UUID postId) {
        likeService.unlike(postId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get like count for a post")
    @GetMapping("/{postId}/likes/count")
    public LikeCountResponse count(@PathVariable UUID postId) {
        return likeService.count(postId);
    }

    @Operation(summary = "Whether the current user liked the post")
    @GetMapping("/{postId}/liked")
    public LikedResponse liked(@PathVariable UUID postId) {
        return likeService.liked(postId);
    }
}
