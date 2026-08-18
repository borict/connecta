package connecta.user.controller;

import connecta.user.config.OpenApiConfig;
import connecta.user.dto.AdminUserResponse;
import connecta.user.dto.PageResponse;
import connecta.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "List users")
    @GetMapping
    public PageResponse<AdminUserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.listUsers(page, size);
    }

    @Operation(summary = "Get user by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/{userId}")
    public AdminUserResponse getUser(@PathVariable UUID userId) {
        return adminUserService.getUser(userId);
    }

    @Operation(summary = "Ban user")
    @PatchMapping("/{userId}/ban")
    public AdminUserResponse ban(@PathVariable UUID userId) {
        return adminUserService.ban(userId);
    }

    @Operation(summary = "Unban user")
    @PatchMapping("/{userId}/unban")
    public AdminUserResponse unban(@PathVariable UUID userId) {
        return adminUserService.unban(userId);
    }

    @Operation(summary = "Deactivate user (soft delete)")
    @PatchMapping("/{userId}/deactivate")
    public AdminUserResponse deactivate(@PathVariable UUID userId) {
        return adminUserService.deactivate(userId);
    }

    @Operation(summary = "Restore deactivated user")
    @PatchMapping("/{userId}/restore")
    public AdminUserResponse restore(@PathVariable UUID userId) {
        return adminUserService.restore(userId);
    }
}
