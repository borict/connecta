package connecta.user.service;

import connecta.user.domain.User;
import connecta.user.dto.AdminUserResponse;
import connecta.user.dto.PageResponse;
import connecta.user.repository.UserRepository;
import connecta.user.security.AuthenticatedUser;
import connecta.user.security.SecurityUtils;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        return PageResponse.from(
                userRepository.findAll(PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .map(AdminUserResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(UUID userId) {
        return AdminUserResponse.from(requireUser(userId));
    }

    @Transactional
    public AdminUserResponse ban(UUID userId) {
        User user = requireMutableTarget(userId);
        user.setBanned(true);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse unban(UUID userId) {
        User user = requireMutableTarget(userId);
        user.setBanned(false);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse deactivate(UUID userId) {
        User user = requireMutableTarget(userId);
        user.setActive(false);
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse restore(UUID userId) {
        User user = requireMutableTarget(userId);
        user.setActive(true);
        return AdminUserResponse.from(userRepository.save(user));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User requireMutableTarget(UUID userId) {
        AuthenticatedUser admin = SecurityUtils.requireCurrentUser();
        if (admin.id().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admins cannot modify their own account via admin API");
        }
        return requireUser(userId);
    }
}
