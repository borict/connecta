package connecta.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.post.domain.Post;
import connecta.post.domain.PostLike;
import connecta.post.domain.Role;
import connecta.post.dto.LikeResponse;
import connecta.post.messaging.PostEventPublisher;
import connecta.post.messaging.PostLikedEvent;
import connecta.post.repository.PostLikeRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository likeRepository;

    @Mock
    private PostEventPublisher eventPublisher;

    private LikeService likeService;
    private UUID userId;
    private UUID postId;
    private Post otherUsersPost;

    @BeforeEach
    void setUp() {
        likeService = new LikeService(postRepository, likeRepository, eventPublisher);
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        otherUsersPost = new Post(postId, UUID.randomUUID(), "Hello");
        AuthenticatedUser user = new AuthenticatedUser(userId, "tamara", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void like_missingPost_returnsNotFound() {
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.like(postId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(likeRepository, never()).save(any());
    }

    @Test
    void like_createsLikeAndPublishesEvent() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);
        when(likeRepository.countByPostId(postId)).thenReturn(1L);

        LikeResponse response = likeService.like(postId);

        verify(likeRepository).save(any(PostLike.class));
        verify(eventPublisher).publishPostLiked(any(PostLikedEvent.class));
        assertThat(response.liked()).isTrue();
        assertThat(response.count()).isEqualTo(1);
    }

    @Test
    void like_ownPost_doesNotPublishEvent() {
        Post ownPost = new Post(postId, userId, "Hello");
        when(postRepository.findById(postId)).thenReturn(Optional.of(ownPost));
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);
        when(likeRepository.countByPostId(postId)).thenReturn(1L);

        likeService.like(postId);

        verify(eventPublisher, never()).publishPostLiked(any());
    }

    @Test
    void like_alreadyLiked_isIdempotent() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(true);
        when(likeRepository.countByPostId(postId)).thenReturn(4L);

        LikeResponse response = likeService.like(postId);

        verify(likeRepository, never()).save(any());
        verify(eventPublisher, never()).publishPostLiked(any());
        assertThat(response.liked()).isTrue();
        assertThat(response.count()).isEqualTo(4);
    }

    @Test
    void like_concurrentDuplicate_isIdempotent() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);
        when(likeRepository.save(any(PostLike.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(likeRepository.countByPostId(postId)).thenReturn(1L);

        LikeResponse response = likeService.like(postId);

        verify(eventPublisher, never()).publishPostLiked(any());
        assertThat(response.liked()).isTrue();
        assertThat(response.count()).isEqualTo(1);
    }

    @Test
    void like_publisherFails_likeStillSucceeds() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);
        when(likeRepository.countByPostId(postId)).thenReturn(1L);
        doThrow(new RuntimeException("bus down")).when(eventPublisher).publishPostLiked(any());

        LikeResponse response = likeService.like(postId);

        assertThat(response.liked()).isTrue();
        assertThat(response.count()).isEqualTo(1);
    }

    @Test
    void unlike_missingLike_isIdempotent() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(likeRepository.deleteByPostIdAndUserId(postId, userId)).thenReturn(0L);

        likeService.unlike(postId);

        verify(likeRepository).deleteByPostIdAndUserId(postId, userId);
    }

    @Test
    void liked_returnsCurrentUserState() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(true);

        assertThat(likeService.liked(postId).liked()).isTrue();
    }
}
