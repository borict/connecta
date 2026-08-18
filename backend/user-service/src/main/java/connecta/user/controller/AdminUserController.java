package connecta.user.controller;

import connecta.user.dto.AdminUserResponse;
import connecta.user.dto.PageResponse;
import connecta.user.service.AdminUserService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PageResponse<AdminUserResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUserService.listUsers(page, size);
    }

    @GetMapping("/{userId}")
    public AdminUserResponse getUser(@PathVariable UUID userId) {
        return adminUserService.getUser(userId);
    }

    @PatchMapping("/{userId}/ban")
    public AdminUserResponse ban(@PathVariable UUID userId) {
        return adminUserService.ban(userId);
    }

    @PatchMapping("/{userId}/unban")
    public AdminUserResponse unban(@PathVariable UUID userId) {
        return adminUserService.unban(userId);
    }

    @PatchMapping("/{userId}/deactivate")
    public AdminUserResponse deactivate(@PathVariable UUID userId) {
        return adminUserService.deactivate(userId);
    }

    @PatchMapping("/{userId}/restore")
    public AdminUserResponse restore(@PathVariable UUID userId) {
        return adminUserService.restore(userId);
    }
}
