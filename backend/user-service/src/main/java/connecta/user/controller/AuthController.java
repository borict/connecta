package connecta.user.controller;

import connecta.user.dto.LoginRequest;
import connecta.user.dto.LoginResponse;
import connecta.user.dto.RegisterRequest;
import connecta.user.dto.UserMeResponse;
import connecta.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserMeResponse> register(
            @Valid @RequestPart("data") RegisterRequest data,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(data, profilePicture));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
