package connecta.post.controller;

import connecta.post.dto.CreatePostRequest;
import connecta.post.dto.PageResponse;
import connecta.post.dto.PostResponse;
import connecta.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "Create a post")
    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody CreatePostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(request));
    }

    @Operation(summary = "List posts by author ids", description = "Comma-separated UUIDs, max 100. Intended for feed aggregation.")
    @GetMapping("/by-authors")
    public PageResponse<PostResponse> listByAuthors(
            @RequestParam("ids") String ids,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return postService.listByAuthors(ids, page, size);
    }

    @Operation(summary = "List posts by user")
    @GetMapping("/user/{userId}")
    public PageResponse<PostResponse> listByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return postService.listByUser(userId, page, size);
    }

    @Operation(summary = "Get a post by id")
    @GetMapping("/{postId}")
    public PostResponse getById(@PathVariable UUID postId) {
        return postService.getById(postId);
    }

    @Operation(summary = "Delete a post", description = "Only the author can delete the post.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable UUID postId) {
        postService.delete(postId);
        return ResponseEntity.noContent().build();
    }
}
