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
class FeedServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private PostClient postClient;

    private FeedService feedService;
    private UUID viewerId;
    private UUID followeeId;

    @BeforeEach
    void setUp() {
        feedService = new FeedService(followRepository, postClient);
        viewerId = UUID.randomUUID();
        followeeId = UUID.randomUUID();
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
    void feed_includesSelfAndAcceptedFollowees() {
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED))
                .thenReturn(List.of(new Follow(viewerId, followeeId, FollowStatus.ACCEPTED)));
        FeedPostDto post = post(followeeId);
        when(postClient.listByAuthors(anyString(), eq(0), eq(20)))
                .thenReturn(new PageResponse<>(List.of(post), 0, 20, 1, 1));

        PageResponse<FeedPostDto> response = feedService.feed(0, 20);

        ArgumentCaptor<String> idsCaptor = ArgumentCaptor.forClass(String.class);
        verify(postClient).listByAuthors(idsCaptor.capture(), eq(0), eq(20));
        List<String> ids = Arrays.asList(idsCaptor.getValue().split(","));
        assertThat(ids.get(0)).isEqualTo(viewerId.toString());
        assertThat(ids).containsExactly(viewerId.toString(), followeeId.toString());
        assertThat(response.content()).containsExactly(post);
        verify(followRepository).findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED);
    }

    @Test
    void feed_withNoFollows_requestsOnlySelf() {
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED)).thenReturn(List.of());
        when(postClient.listByAuthors(eq(viewerId.toString()), eq(0), eq(20)))
                .thenReturn(PageResponse.empty(0, 20));

        feedService.feed(0, 20);

        verify(postClient).listByAuthors(eq(viewerId.toString()), eq(0), eq(20));
    }

    @Test
    void feed_clampsPagination() {
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED)).thenReturn(List.of());
        when(postClient.listByAuthors(anyString(), eq(0), eq(50)))
                .thenReturn(PageResponse.empty(0, 50));

        feedService.feed(-2, 999);

        verify(postClient).listByAuthors(anyString(), eq(0), eq(50));
    }

    @Test
    void feed_capsAuthorIdsAt100() {
        List<Follow> follows = IntStream.range(0, 120)
                .mapToObj(i -> new Follow(viewerId, UUID.randomUUID(), FollowStatus.ACCEPTED))
                .toList();
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED)).thenReturn(follows);
        when(postClient.listByAuthors(anyString(), eq(0), eq(20)))
                .thenReturn(PageResponse.empty(0, 20));

        feedService.feed(0, 20);

        ArgumentCaptor<String> idsCaptor = ArgumentCaptor.forClass(String.class);
        verify(postClient).listByAuthors(idsCaptor.capture(), eq(0), eq(20));
        String[] ids = idsCaptor.getValue().split(",");
        assertThat(ids).hasSize(100);
        assertThat(ids[0]).isEqualTo(viewerId.toString());
    }

    @Test
    void feed_postServiceDown_returnsEmptyPage() {
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED)).thenReturn(List.of());
        when(postClient.listByAuthors(anyString(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("connection refused"));

        PageResponse<FeedPostDto> response = feedService.feed(0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isZero();
        verify(postClient).listByAuthors(anyString(), eq(0), eq(20));
        verify(postClient, never()).listByAuthors(eq(""), anyInt(), anyInt());
    }

    @Test
    void feed_postServiceReturnsNull_returnsEmptyPage() {
        when(followRepository.findByFollowerIdAndStatus(viewerId, FollowStatus.ACCEPTED)).thenReturn(List.of());
        when(postClient.listByAuthors(anyString(), eq(1), eq(10))).thenReturn(null);

        PageResponse<FeedPostDto> response = feedService.feed(1, 10);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
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
