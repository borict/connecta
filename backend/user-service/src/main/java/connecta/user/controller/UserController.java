package connecta.user.controller;

import connecta.user.dto.PageResponse;
import connecta.user.dto.UpdateProfileRequest;
import connecta.user.dto.UserMeResponse;
import connecta.user.dto.UserSummaryResponse;
import connecta.user.service.UserService;
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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserMeResponse me() {
        return userService.getMe();
    }

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

    @GetMapping("/{userId}")
    public Object getById(@PathVariable UUID userId) {
        return userService.getById(userId);
    }

    @GetMapping("/by-username/{username}")
    public Object getByUsername(@PathVariable String username) {
        return userService.getByUsername(username);
    }

    @GetMapping("/search")
    public PageResponse<UserSummaryResponse> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userService.search(query, page, size);
    }

    @GetMapping("/batch")
    public List<UserSummaryResponse> batch(@RequestParam("ids") String ids) {
        return userService.batch(ids);
    }
}
