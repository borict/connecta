package connecta.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.post.domain.Post;
import connecta.post.domain.Role;
import connecta.post.dto.CreatePostRequest;
import connecta.post.dto.PageResponse;
import connecta.post.dto.PostResponse;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    private PostService postService;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository);
        authorId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(authorId, "tamara", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_persistsTrimmedContentForCurrentUser() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.create(new CreatePostRequest("  Hello Connecta!  "));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getAuthorId()).isEqualTo(authorId);
        assertThat(saved.getContent()).isEqualTo("Hello Connecta!");
        assertThat(saved.getImageUrl()).isNull();
        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.content()).isEqualTo("Hello Connecta!");
        assertThat(response.likeCount()).isZero();
        assertThat(response.commentCount()).isZero();
    }

    @Test
    void getById_missingPost_returnsNotFound() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getById(postId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(statusEx.getReason()).isEqualTo("Post not found");
                });
    }

    @Test
    void delete_author_deletesPost() {
        UUID postId = UUID.randomUUID();
        Post post = new Post(postId, authorId, "Hello");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.delete(postId);

        verify(postRepository).delete(post);
    }

    @Test
    void delete_nonAuthor_returnsForbidden() {
        UUID postId = UUID.randomUUID();
        Post post = new Post(postId, UUID.randomUUID(), "Hello");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.delete(postId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(statusEx.getReason()).isEqualTo("Only the author can delete this post");
                });
        verify(postRepository, never()).delete(any());
    }

    @Test
    void listByUser_clampsPagination() {
        UUID userId = UUID.randomUUID();
        when(postRepository.findByAuthorIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        PageResponse<PostResponse> response = postService.listByUser(userId, -2, 999);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findByAuthorIdOrderByCreatedAtDesc(eq(userId), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(50);
    }

    @Test
    void listByAuthors_invalidId_returnsBadRequest() {
        assertThatThrownBy(() -> postService.listByAuthors("not-a-uuid", 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).contains("Invalid user id");
                });
    }

    @Test
    void listByAuthors_tooManyIds_returnsBadRequest() {
        String ids = IntStream.range(0, 101)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.joining(","));

        assertThatThrownBy(() -> postService.listByAuthors(ids, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(statusEx.getReason()).isEqualTo("At most 100 ids are allowed");
                });
    }
}
