package connecta.message.controller;

import connecta.message.config.OpenApiConfig;
import connecta.message.dto.ConversationResponse;
import connecta.message.dto.PageResponse;
import connecta.message.service.ConversationService;
import connecta.message.service.ConversationService.ConversationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Conversations", description = "1:1 direct-message conversations")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(
            summary = "List current user's conversations",
            description = "Newest activity first. page is 0-based; size defaults to 20 (max 50). "
                    + "otherUsername is null if User Service is down."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            )
    })
    @GetMapping
    public PageResponse<ConversationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return conversationService.list(page, size);
    }

    @Operation(
            summary = "Create or return the 1:1 conversation with userId",
            description = "201 when created, 200 when the pair already exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Existing conversation"),
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot message yourself",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "User Service unavailable",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            )
    })
    @PostMapping("/users/{userId}")
    public ResponseEntity<ConversationResponse> getOrCreate(@PathVariable UUID userId) {
        ConversationResult result = conversationService.getOrCreate(userId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.conversation());
    }

    @Operation(summary = "Conversation with userId", description = "404 if no 1:1 conversation exists yet.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot message yourself",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Conversation not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            )
    })
    @GetMapping("/users/{userId}")
    public ConversationResponse getWithUser(@PathVariable UUID userId) {
        return conversationService.getWithUser(userId);
    }
}
