package connecta.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.post.domain.PostLike;
import connecta.post.domain.Role;
import connecta.post.dto.LikeResponse;
import connecta.post.repository.PostLikeRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
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

    private LikeService likeService;
    private UUID userId;
    private UUID postId;

    @BeforeEach
    void setUp() {
        likeService = new LikeService(postRepository, likeRepository);
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
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
        when(postRepository.existsById(postId)).thenReturn(false);

        assertThatThrownBy(() -> likeService.like(postId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(likeRepository, never()).save(any());
    }

    @Test
    void like_createsLike() {
        when(postRepository.existsById(postId)).thenReturn(true);
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);
        when(likeRepository.countByPostId(postId)).thenReturn(1L);

        LikeResponse response = likeService.like(postId);

        verify(likeRepository).save(any(PostLike.class));
        assertThat(response.liked()).isTrue();
        assertThat(response.count()).isEqualTo(1);
    }

    @Test
    void like_alreadyLiked_isIdempotent() {
        when(postRepository.existsById(postId)).thenReturn(true);
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(true);
        when(likeRepository.countByPostId(postId)).thenReturn(4L);

        LikeResponse response = likeService.like(postId);

        verify(likeRepository, never()).save(any());
        assertThat(response.liked()).isTrue();
        assertThat(response.count()).isEqualTo(4);
    }

    @Test
    void like_concurrentDuplicate_isIdempotent() {
        when(postRepository.existsById(postId)).thenReturn(true);
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);
        when(likeRepository.save(any(PostLike.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(likeRepository.countByPostId(postId)).thenReturn(1L);

        LikeResponse response = likeService.like(postId);

        assertThat(response.liked()).isTrue();
        assertThat(response.count()).isEqualTo(1);
    }

    @Test
    void unlike_missingLike_isIdempotent() {
        when(postRepository.existsById(postId)).thenReturn(true);
        when(likeRepository.deleteByPostIdAndUserId(postId, userId)).thenReturn(0L);

        likeService.unlike(postId);

        verify(likeRepository).deleteByPostIdAndUserId(postId, userId);
    }

    @Test
    void liked_returnsCurrentUserState() {
        when(postRepository.existsById(postId)).thenReturn(true);
        when(likeRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(true);

        assertThat(likeService.liked(postId).liked()).isTrue();
    }
}
