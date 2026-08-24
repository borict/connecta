package connecta.post.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import connecta.post.config.OpenApiConfig;
import connecta.post.dto.CreatePostRequest;
import connecta.post.dto.PageResponse;
import connecta.post.dto.PostResponse;
import connecta.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts", description = "Create, read and author-only delete")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PostController {

    private final PostService postService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PostController(PostService postService, ObjectMapper objectMapper, Validator validator) {
        this.postService = postService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Operation(
            summary = "Create a post",
            description = """
                    Multipart form:
                    - `data` (required): JSON CreatePostRequest
                    - `image` (optional): one JPEG, PNG, or WebP file, max 5MB
                    
                    In Swagger: paste JSON into `data`, leave image empty,
                    and do NOT check "Send empty value".
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CreatePostMultipartRequest.class),
                    encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> create(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        CreatePostRequest data = parseCreatePostRequest(dataJson);
        validateCreatePostRequest(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.create(data, image));
    }

    @Operation(summary = "List posts by author ids", description = "Comma-separated UUIDs, max 100. Intended for feed aggregation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid ids", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/by-authors")
    public PageResponse<PostResponse> listByAuthors(
            @RequestParam("ids") String ids,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return postService.listByAuthors(ids, page, size);
    }

    @Operation(
            summary = "List posts by user",
            description = "Returns 403 when the author's profile is private and the viewer is not an ACCEPTED follower."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "Private profile", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/user/{userId}")
    public PageResponse<PostResponse> listByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return postService.listByUser(userId, page, size);
    }

    @Operation(summary = "Get a post by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/{postId}")
    public PostResponse getById(@PathVariable UUID postId) {
        return postService.getById(postId);
    }

    @Operation(summary = "Delete a post", description = "Only the author can delete the post.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "Not the author", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable UUID postId) {
        postService.delete(postId);
        return ResponseEntity.noContent().build();
    }

    private CreatePostRequest parseCreatePostRequest(String dataJson) {
        try {
            return objectMapper.readValue(dataJson, CreatePostRequest.class);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in data part");
        }
    }

    private void validateCreatePostRequest(CreatePostRequest data) {
        var violations = validator.validate(data);
        if (violations.isEmpty()) {
            return;
        }
        String message = violations.stream()
                .map(this::formatViolation)
                .collect(Collectors.joining("; "));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String formatViolation(ConstraintViolation<CreatePostRequest> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    @Schema(name = "CreatePostMultipartRequest")
    public static class CreatePostMultipartRequest {
        @Schema(implementation = CreatePostRequest.class, requiredMode = Schema.RequiredMode.REQUIRED)
        public CreatePostRequest data;

        @Schema(type = "string", format = "binary", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        public String image;
    }
}
