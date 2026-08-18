package connecta.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import connecta.user.dto.LoginRequest;
import connecta.user.dto.LoginResponse;
import connecta.user.dto.RegisterRequest;
import connecta.user.dto.UserMeResponse;
import connecta.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AuthController(AuthService authService, ObjectMapper objectMapper, Validator validator) {
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Operation(
            summary = "Register a new user",
            description = """
                    Multipart form:
                    - `data` (required): JSON RegisterRequest
                    - `profilePicture` (optional): image file
                    
                    In Swagger: paste a real JSON example into `data`, leave profilePicture empty,
                    and do NOT check "Send empty value".
                    Role is always USER.
                    """
    )
    @RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = RegisterMultipartRequest.class),
                    encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "409", description = "Username or email taken", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserMeResponse> register(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture
    ) {
        RegisterRequest data = parseRegisterRequest(dataJson);
        validateRegisterRequest(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(data, profilePicture));
    }

    @Operation(summary = "Login", description = "Returns JWT and current user. Rejects banned/inactive accounts.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse"))),
            @ApiResponse(responseCode = "403", description = "Banned or deactivated", content = @Content(schema = @Schema(ref = "#/components/schemas/ApiErrorResponse")))
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public LoginResponse login(@Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest request) {
        return authService.login(request);
    }

    private RegisterRequest parseRegisterRequest(String dataJson) {
        try {
            return objectMapper.readValue(dataJson, RegisterRequest.class);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in data part");
        }
    }

    private void validateRegisterRequest(RegisterRequest data) {
        var violations = validator.validate(data);
        if (violations.isEmpty()) {
            return;
        }
        String message = violations.stream()
                .map(this::formatViolation)
                .collect(Collectors.joining("; "));
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String formatViolation(ConstraintViolation<RegisterRequest> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    /**
     * Swagger-only schema so the UI shows JSON `data` + optional file.
     */
    @Schema(name = "RegisterMultipartRequest")
    public static class RegisterMultipartRequest {
        @Schema(implementation = RegisterRequest.class, requiredMode = Schema.RequiredMode.REQUIRED)
        public RegisterRequest data;

        @Schema(type = "string", format = "binary", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        public String profilePicture;
    }
}
