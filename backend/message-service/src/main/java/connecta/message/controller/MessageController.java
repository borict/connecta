package connecta.message.controller;

import connecta.message.config.OpenApiConfig;
import connecta.message.dto.CreateMessageRequest;
import connecta.message.dto.MessageResponse;
import connecta.message.dto.PageResponse;
import connecta.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "Messages", description = "Direct messages in a 1:1 conversation")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(
            summary = "List messages with userId",
            description = "Newest first. page is 0-based; size defaults to 20 (max 50)."
    )
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
    @GetMapping("/users/{userId}/messages")
    public PageResponse<MessageResponse> list(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return messageService.list(userId, page, size);
    }

    @Operation(summary = "Send a message", description = "HTTP fallback for WebSocket send. Conversation must already exist.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
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
    @PostMapping("/users/{userId}/messages")
    public ResponseEntity<MessageResponse> send(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.send(userId, request));
    }

    @Operation(
            summary = "Mark the conversation as read",
            description = "Sets last_read_at for the current user. Idempotent."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content"),
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
    @PutMapping("/users/{userId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID userId) {
        messageService.markRead(userId);
        return ResponseEntity.noContent().build();
    }
}
