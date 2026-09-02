package connecta.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.user.domain.Role;
import connecta.user.domain.User;
import connecta.user.dto.PublicIdsResponse;
import connecta.user.repository.UserRepository;
import connecta.user.security.AuthenticatedUser;
import connecta.user.storage.ProfilePictureStorage;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfilePictureStorage profilePictureStorage;

    @Mock
    private ProfileVisibilityService profileVisibility;

    private UserService userService;
    private UUID viewerId;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, profilePictureStorage, profileVisibility);
        viewerId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(viewerId, "tamara", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicIds_excludesViewerAndCapsAt100() {
        User viewer = activeUser(viewerId, "tamara");
        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        UUID other = UUID.randomUUID();
        when(userRepository.findPublicIdsExcluding(eq(viewerId), any(Pageable.class)))
                .thenReturn(List.of(other));

        PublicIdsResponse response = userService.publicIds();

        assertThat(response.ids()).containsExactly(other);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findPublicIdsExcluding(eq(viewerId), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(UserService.MAX_PUBLIC_IDS);
    }

    @Test
    void publicIds_emptyWhenNobodyElseIsPublic() {
        when(userRepository.findById(viewerId)).thenReturn(Optional.of(activeUser(viewerId, "tamara")));
        when(userRepository.findPublicIdsExcluding(eq(viewerId), any(Pageable.class)))
                .thenReturn(List.of());

        assertThat(userService.publicIds().ids()).isEmpty();
    }

    @Test
    void publicIds_bannedViewerForbidden() {
        User banned = activeUser(viewerId, "tamara");
        banned.setBanned(true);
        when(userRepository.findById(viewerId)).thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> userService.publicIds())
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static User activeUser(UUID id, String username) {
        return new User(id, username, username + "@example.com", "hash", "Name", LocalDate.of(2000, 1, 1));
    }
}
