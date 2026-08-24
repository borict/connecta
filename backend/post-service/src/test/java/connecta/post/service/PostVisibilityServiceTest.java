package connecta.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.post.client.FollowStateDto;
import connecta.post.client.FollowingIdsDto;
import connecta.post.client.SocialClient;
import connecta.post.domain.Post;
import connecta.post.domain.Role;
import connecta.post.dto.AuthorSummary;
import connecta.post.security.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PostVisibilityServiceTest {

    @Mock
    private AuthorEnrichmentService authorEnrichment;

    @Mock
    private SocialClient socialClient;

    private PostVisibilityService visibility;
    private UUID viewerId;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        visibility = new PostVisibilityService(authorEnrichment, socialClient);
        viewerId = UUID.randomUUID();
        authorId = UUID.randomUUID();
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
    void ownerCanSeeOwnPrivatePostsWithoutSocialCall() {
        assertThat(visibility.canSeeAuthor(viewerId)).isTrue();
        verify(socialClient, never()).isFollowing(viewerId);
    }

    @Test
    void publicAuthorIsVisibleWithoutSocialCall() {
        when(authorEnrichment.byIds(List.of(authorId))).thenReturn(Map.of(
                authorId,
                new AuthorSummary(authorId, "ana", "Ana", null, false)
        ));

        assertThat(visibility.canSeeAuthor(authorId)).isTrue();
        verify(socialClient, never()).isFollowing(authorId);
    }

    @Test
    void acceptedFollowerCanSeePrivateAuthor() {
        when(authorEnrichment.byIds(List.of(authorId))).thenReturn(Map.of(
                authorId,
                new AuthorSummary(authorId, "ana", "Ana", null, true)
        ));
        when(socialClient.isFollowing(authorId)).thenReturn(new FollowStateDto(true, false));

        assertThat(visibility.canSeeAuthor(authorId)).isTrue();
    }

    @Test
    void hiddenPostLooksMissing() {
        Post post = new Post(UUID.randomUUID(), authorId, "Hello");
        when(authorEnrichment.byIds(List.of(authorId))).thenReturn(Map.of(
                authorId,
                new AuthorSummary(authorId, "ana", "Ana", null, true)
        ));
        when(socialClient.isFollowing(authorId)).thenReturn(new FollowStateDto(false, true));

        assertThatThrownBy(() -> visibility.requireVisiblePost(post))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("Post not found");
                });
    }

    @Test
    void hiddenProfilePostsAreForbidden() {
        when(authorEnrichment.byIds(List.of(authorId))).thenReturn(Map.of(
                authorId,
                new AuthorSummary(authorId, "ana", "Ana", null, true)
        ));
        when(socialClient.isFollowing(authorId)).thenReturn(new FollowStateDto(false, false));

        assertThatThrownBy(() -> visibility.requireVisibleProfilePosts(authorId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(statusEx.getReason()).isEqualTo("This profile is private");
                });
    }

    @Test
    void userDownTreatsAuthorAsPrivate() {
        when(authorEnrichment.byIds(List.of(authorId))).thenReturn(Map.of());
        when(socialClient.isFollowing(authorId)).thenReturn(new FollowStateDto(false, false));

        assertThat(visibility.canSeeAuthor(authorId)).isFalse();
    }

    @Test
    void socialDownHidesPrivateAuthor() {
        when(authorEnrichment.byIds(List.of(authorId))).thenReturn(Map.of(
                authorId,
                new AuthorSummary(authorId, "ana", "Ana", null, true)
        ));
        when(socialClient.isFollowing(authorId)).thenThrow(new RuntimeException("connection refused"));

        assertThat(visibility.canSeeAuthor(authorId)).isFalse();
    }

    @Test
    void visibleAuthorIdsUsesFollowingBatchAndKeepsPublicAndSelf() {
        UUID publicId = UUID.randomUUID();
        UUID followedPrivateId = UUID.randomUUID();
        UUID hiddenId = UUID.randomUUID();
        when(authorEnrichment.byIds(List.of(publicId, followedPrivateId, hiddenId, viewerId))).thenReturn(Map.of(
                publicId, new AuthorSummary(publicId, "public", "Public", null, false),
                followedPrivateId, new AuthorSummary(followedPrivateId, "priv", "Priv", null, true),
                hiddenId, new AuthorSummary(hiddenId, "hidden", "Hidden", null, true),
                viewerId, new AuthorSummary(viewerId, "tamara", "Tamara", null, true)
        ));
        when(socialClient.followingIds()).thenReturn(new FollowingIdsDto(List.of(followedPrivateId)));

        List<UUID> visible = visibility.visibleAuthorIds(
                List.of(publicId, followedPrivateId, hiddenId, viewerId)
        );

        assertThat(visible).containsExactly(publicId, followedPrivateId, viewerId);
        verify(socialClient, never()).isFollowing(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void visibleAuthorIdsSkipsSocialWhenEveryAuthorIsPublic() {
        UUID publicId = UUID.randomUUID();
        when(authorEnrichment.byIds(List.of(publicId))).thenReturn(Map.of(
                publicId, new AuthorSummary(publicId, "public", "Public", null, false)
        ));

        assertThat(visibility.visibleAuthorIds(List.of(publicId))).containsExactly(publicId);
        verify(socialClient, never()).followingIds();
    }
}
