package connecta.user.service;

import connecta.user.domain.Role;
import connecta.user.domain.User;
import connecta.user.dto.LoginRequest;
import connecta.user.dto.LoginResponse;
import connecta.user.dto.RegisterRequest;
import connecta.user.dto.UserMeResponse;
import connecta.user.repository.UserRepository;
import connecta.user.security.JwtService;
import connecta.user.storage.ProfilePictureStorage;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ProfilePictureStorage profilePictureStorage;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ProfilePictureStorage profilePictureStorage
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.profilePictureStorage = profilePictureStorage;
    }

    @Transactional
    public UserMeResponse register(RegisterRequest request, MultipartFile profilePicture) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already taken");
        }

        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                request.username().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                request.dateOfBirth()
        );
        user.setRole(Role.USER);
        user.setBio(blankToNull(request.bio()));
        user.setLocation(blankToNull(request.location()));
        user.setGender(request.gender());
        if (request.isPrivate() != null) {
            user.setPrivate(request.isPrivate());
        }

        if (profilePicture != null && !profilePicture.isEmpty()) {
            user.setProfilePictureUrl(profilePictureStorage.store(userId, profilePicture));
        }

        return UserMeResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmailIgnoreCase(request.usernameOrEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }
        if (user.isBanned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }

        return new LoginResponse(jwtService.createToken(user), UserMeResponse.from(user));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
