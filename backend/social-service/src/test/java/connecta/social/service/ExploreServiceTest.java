package connecta.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.social.client.FeedPostDto;
import connecta.social.client.PostClient;
import connecta.social.client.PublicIdsDto;
import connecta.social.client.UserClient;
import connecta.social.domain.Follow;
import connecta.social.domain.FollowStatus;
import connecta.social.domain.Role;
import connecta.social.dto.PageResponse;
import connecta.social.repository.FollowRepository;
import connecta.social.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ExploreServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private PostClient postClient;

    private ExploreService exploreService;
    private UUID viewerId;
    private UUID publicAuthorId;
    private UUID followedId;

    @BeforeEach
    void setUp() {
        exploreService = new ExploreService(userClient, followRepository, postClient);
        viewerId = UUID.randomUUID();
        publicAuthorId = UUID.randomUUID();
        followedId = UUID.randomUUID();
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
    void explore_excludesSelfAndAcceptedFollowees() {
        when(userClient.publicIds()).thenReturn(new PublicIdsDto(List.of(publicAuthorId, followedId, viewerId)));
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED))
                .thenReturn(List.of(new Follow(viewerId, followedId, FollowStatus.ACCEPTED)));
        FeedPostDto post = post(publicAuthorId);
        when(postClient.listByAuthors(eq(publicAuthorId.toString()), eq(0), eq(20)))
                .thenReturn(new PageResponse<>(List.of(post), 0, 20, 1, 1));

        PageResponse<FeedPostDto> response = exploreService.explore(0, 20);

        assertThat(response.content()).containsExactly(post);
        verify(postClient).listByAuthors(eq(publicAuthorId.toString()), eq(0), eq(20));
    }

    @Test
    void explore_noCandidates_doesNotCallPostService() {
        when(userClient.publicIds()).thenReturn(new PublicIdsDto(List.of(followedId)));
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED))
                .thenReturn(List.of(new Follow(viewerId, followedId, FollowStatus.ACCEPTED)));

        PageResponse<FeedPostDto> response = exploreService.explore(0, 20);

        assertThat(response.content()).isEmpty();
        verify(postClient, never()).listByAuthors(anyString(), anyInt(), anyInt());
    }

    @Test
    void explore_userServiceDown_returnsEmptyPage() {
        when(userClient.publicIds()).thenThrow(new RuntimeException("connection refused"));

        PageResponse<FeedPostDto> response = exploreService.explore(0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        verify(postClient, never()).listByAuthors(anyString(), anyInt(), anyInt());
        verify(followRepository, never()).findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED);
    }

    @Test
    void explore_postServiceDown_returnsEmptyPage() {
        when(userClient.publicIds()).thenReturn(new PublicIdsDto(List.of(publicAuthorId)));
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED)).thenReturn(List.of());
        when(postClient.listByAuthors(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("connection refused"));

        PageResponse<FeedPostDto> response = exploreService.explore(0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.size()).isEqualTo(20);
        verify(postClient).listByAuthors(eq(publicAuthorId.toString()), eq(0), eq(20));
    }

    @Test
    void explore_clampsPagination() {
        when(userClient.publicIds()).thenReturn(new PublicIdsDto(List.of(publicAuthorId)));
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED)).thenReturn(List.of());
        when(postClient.listByAuthors(anyString(), eq(0), eq(50)))
                .thenReturn(PageResponse.empty(0, 50));

        exploreService.explore(-2, 999);

        verify(postClient).listByAuthors(eq(publicAuthorId.toString()), eq(0), eq(50));
    }

    @Test
    void explore_capsAuthorIdsAt100() {
        List<UUID> many = IntStream.range(0, 120).mapToObj(i -> UUID.randomUUID()).toList();
        when(userClient.publicIds()).thenReturn(new PublicIdsDto(many));
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED)).thenReturn(List.of());
        when(postClient.listByAuthors(anyString(), eq(0), eq(20)))
                .thenReturn(PageResponse.empty(0, 20));

        exploreService.explore(0, 20);

        ArgumentCaptor<String> idsCaptor = ArgumentCaptor.forClass(String.class);
        verify(postClient).listByAuthors(idsCaptor.capture(), eq(0), eq(20));
        assertThat(idsCaptor.getValue().split(",")).hasSize(100);
        assertThat(Arrays.asList(idsCaptor.getValue().split(",")).get(0)).isEqualTo(many.get(0).toString());
    }

    private static FeedPostDto post(UUID authorId) {
        return new FeedPostDto(
                UUID.randomUUID(),
                authorId,
                "ana",
                "Ana",
                null,
                "Hello",
                null,
                0,
                0,
                Instant.parse("2026-08-24T16:00:00Z")
        );
    }
}
