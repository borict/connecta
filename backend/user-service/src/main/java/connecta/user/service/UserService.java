package connecta.user.service;

import connecta.user.domain.User;
import connecta.user.dto.PageResponse;
import connecta.user.dto.UpdateProfileRequest;
import connecta.user.dto.UserLimitedResponse;
import connecta.user.dto.UserMeResponse;
import connecta.user.dto.UserPublicResponse;
import connecta.user.dto.UserSummaryResponse;
import connecta.user.repository.UserRepository;
import connecta.user.security.AuthenticatedUser;
import connecta.user.security.SecurityUtils;
import connecta.user.storage.ProfilePictureStorage;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProfilePictureStorage profilePictureStorage;
    private final ProfileVisibilityService profileVisibility;

    public UserService(
            UserRepository userRepository,
            ProfilePictureStorage profilePictureStorage,
            ProfileVisibilityService profileVisibility
    ) {
        this.userRepository = userRepository;
        this.profilePictureStorage = profilePictureStorage;
        this.profileVisibility = profileVisibility;
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe() {
        return UserMeResponse.from(requireActiveUser(SecurityUtils.requireCurrentUser().id()));
    }

    @Transactional
    public UserMeResponse updateMe(UpdateProfileRequest request, MultipartFile profilePicture) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        User user = requireActiveUser(currentUser.id());

        if (request != null) {
            if (request.displayName() != null) {
                String displayName = request.displayName().trim();
                if (displayName.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name must not be blank");
                }
                user.setDisplayName(displayName);
            }
            if (request.bio() != null) {
                user.setBio(blankToNull(request.bio()));
            }
            if (request.location() != null) {
                user.setLocation(blankToNull(request.location()));
            }
            if (request.gender() != null) {
                user.setGender(request.gender());
            }
            if (request.isPrivate() != null) {
                user.setPrivate(request.isPrivate());
            }
        }

        if (profilePicture != null && !profilePicture.isEmpty()) {
            user.setProfilePictureUrl(profilePictureStorage.store(user.getId(), profilePicture));
        }

        return UserMeResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Object getById(UUID userId) {
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toProfileView(user, SecurityUtils.requireCurrentUser());
    }

    @Transactional(readOnly = true)
    public Object getByUsername(String username) {
        User user = userRepository.findByUsernameIgnoreCaseAndIsActiveTrue(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toProfileView(user, SecurityUtils.requireCurrentUser());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> search(String query, int page, int size) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query must be at least 2 characters");
        }
        int safeSize = size <= 0 ? 20 : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        return PageResponse.from(
                userRepository.searchByUsernameOrDisplayName(query.trim(), PageRequest.of(safePage, safeSize))
                        .map(UserSummaryResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> batch(String ids) {
        if (ids == null || ids.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids query parameter is required");
        }
        List<UUID> userIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return UUID.fromString(value);
                    } catch (IllegalArgumentException ex) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id: " + value);
                    }
                })
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids query parameter is required");
        }
        if (userIds.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most 100 ids are allowed");
        }
        return userRepository.findByIdInAndIsActiveTrue(userIds).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    /**
     * Private profiles return limited fields unless the viewer is the owner
     * or an ACCEPTED follower.
     */
    private Object toProfileView(User user, AuthenticatedUser viewer) {
        if (profileVisibility.canSeeFullProfile(user, viewer)) {
            return UserPublicResponse.from(user);
        }
        return UserLimitedResponse.from(user);
    }

    private User requireActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }
        if (user.isBanned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is banned");
        }
        return user;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
