package connecta.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.user.client.FollowStateDto;
import connecta.user.client.SocialClient;
import connecta.user.domain.Role;
import connecta.user.domain.User;
import connecta.user.security.AuthenticatedUser;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileVisibilityServiceTest {

    @Mock
    private SocialClient socialClient;

    private ProfileVisibilityService visibility;
    private UUID ownerId;
    private UUID viewerId;
    private AuthenticatedUser viewer;

    @BeforeEach
    void setUp() {
        visibility = new ProfileVisibilityService(socialClient);
        ownerId = UUID.randomUUID();
        viewerId = UUID.randomUUID();
        viewer = new AuthenticatedUser(viewerId, "tamara", Role.USER);
    }

    @Test
    void ownerSeesFullPrivateProfileWithoutSocialCall() {
        assertThat(visibility.canSeeFullProfile(user(ownerId, true), ownerViewer())).isTrue();
        verify(socialClient, never()).isFollowing(ownerId);
    }

    @Test
    void publicProfileIsFullWithoutSocialCall() {
        assertThat(visibility.canSeeFullProfile(user(ownerId, false), viewer)).isTrue();
        verify(socialClient, never()).isFollowing(ownerId);
    }

    @Test
    void acceptedFollowerSeesFullPrivateProfile() {
        when(socialClient.isFollowing(ownerId)).thenReturn(new FollowStateDto(true, false));

        assertThat(visibility.canSeeFullProfile(user(ownerId, true), viewer)).isTrue();
    }

    @Test
    void pendingRequestSeesLimitedPrivateProfile() {
        when(socialClient.isFollowing(ownerId)).thenReturn(new FollowStateDto(false, true));

        assertThat(visibility.canSeeFullProfile(user(ownerId, true), viewer)).isFalse();
    }

    @Test
    void socialDownHidesPrivateProfile() {
        when(socialClient.isFollowing(ownerId)).thenThrow(new RuntimeException("connection refused"));

        assertThat(visibility.canSeeFullProfile(user(ownerId, true), viewer)).isFalse();
    }

    private AuthenticatedUser ownerViewer() {
        return new AuthenticatedUser(ownerId, "ana", Role.USER);
    }

    private static User user(UUID id, boolean isPrivate) {
        User user = new User(id, "ana", "ana@example.com", "hash", "Ana", LocalDate.of(2000, 1, 1));
        user.setPrivate(isPrivate);
        return user;
    }
}
