package connecta.user.controller;

import connecta.user.dto.PageResponse;
import connecta.user.dto.UpdateProfileRequest;
import connecta.user.dto.UserLimitedResponse;
import connecta.user.dto.UserMeResponse;
import connecta.user.dto.UserPublicResponse;
import connecta.user.dto.UserSummaryResponse;
import connecta.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import connecta.user.config.OpenApiConfig;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get current user profile", tags = {"Profiles"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @GetMapping("/me")
    public UserMeResponse me() {
        return userService.getMe();
    }

    @Operation(summary = "Update current user profile", description = "Multipart: optional JSON `data` and/or `profilePicture`.", tags = {"Profiles"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserMeResponse updateMe(
            @Valid @RequestPart(value = "data", required = false) UpdateProfileRequest data,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture
    ) {
        if (data == null && (profilePicture == null || profilePicture.isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide profile data and/or a profile picture"
            );
        }
        return userService.updateMe(data, profilePicture);
    }

    @Operation(
            summary = "Get user by id",
            description = "Public profile, or limited fields when the target profile is private and the viewer is not the owner.",
            tags = {"Profiles"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK", content = {
                            @Content(mediaType = "application/json", schema = @Schema(oneOf = {
                                    UserPublicResponse.class,
                                    UserLimitedResponse.class
                            }))
                    }),
                    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
            }
    )
    @GetMapping("/{userId}")
    public Object getById(@PathVariable UUID userId) {
        return userService.getById(userId);
    }

    @Operation(summary = "Get user by username", tags = {"Profiles"})
    @GetMapping("/by-username/{username}")
    public Object getByUsername(@PathVariable String username) {
        return userService.getByUsername(username);
    }

    @Operation(summary = "Search users", description = "Query must be at least 2 characters.")
    @GetMapping("/search")
    public PageResponse<UserSummaryResponse> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userService.search(query, page, size);
    }

    @Operation(summary = "Batch get users by ids", description = "Comma-separated UUIDs, max 100. Intended for enrichment.", tags = {"Internal"})
    @GetMapping("/batch")
    public List<UserSummaryResponse> batch(@RequestParam("ids") String ids) {
        return userService.batch(ids);
    }
}
