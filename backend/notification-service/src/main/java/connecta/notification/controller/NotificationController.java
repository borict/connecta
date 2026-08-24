package connecta.notification.controller;

import connecta.notification.config.OpenApiConfig;
import connecta.notification.dto.NotificationResponse;
import connecta.notification.dto.PageResponse;
import connecta.notification.dto.UnreadCountResponse;
import connecta.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Current user's in-app notifications")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "List notifications",
            description = "Newest first. page is 0-based; size defaults to 20 (max 50)."
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
    public PageResponse<NotificationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.list(page, size);
    }

    @Operation(summary = "Unread notification count")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            )
    })
    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return notificationService.unreadCount();
    }

    @Operation(
            summary = "Mark one notification as read",
            description = "Idempotent. Another user's id returns 404 (not 403)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            )
    })
    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable UUID id) {
        return notificationService.markRead(id);
    }

    @Operation(summary = "Mark all notifications as read")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))
            )
    })
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }
}
