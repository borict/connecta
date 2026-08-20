package connecta.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.post.domain.Post;
import connecta.post.domain.Role;
import connecta.post.dto.AuthorSummary;
import connecta.post.dto.CreatePostRequest;
import connecta.post.dto.PageResponse;
import connecta.post.dto.PostResponse;
import connecta.post.repository.CommentRepository;
import connecta.post.repository.PostLikeRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import connecta.post.storage.PostImageStorage;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository likeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostImageStorage postImageStorage;

    @Mock
    private AuthorEnrichmentService authorEnrichment;

    private PostService postService;
    private UUID authorId;

    @BeforeEach
    void setUp() {
        postService = new PostService(
                postRepository,
                likeRepository,
                commentRepository,
                postImageStorage,
                authorEnrichment
        );
        authorId = UUID.randomUUID();
        AuthenticatedUser user = new AuthenticatedUser(authorId, "tamara", Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
        lenient().when(authorEnrichment.byIds(any())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            if (ids == null) {
                return Map.of();
            }
            return ids.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toMap(id -> id, AuthorSummary::fallback, (left, right) -> left));
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_persistsTrimmedContentForCurrentUser() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PostResponse response = postService.create(new CreatePostRequest("  Hello Connecta!  "), null);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getAuthorId()).isEqualTo(authorId);
        assertThat(saved.getContent()).isEqualTo("Hello Connecta!");
        assertThat(saved.getImageUrl()).isNull();
        assertThat(response.authorId()).isEqualTo(authorId);
        assertThat(response.authorUsername()).isNull();
        assertThat(response.content()).isEqualTo("Hello Connecta!");
        assertThat(response.likeCount()).isZero();
        assertThat(response.commentCount()).isZero();
        verify(postImageStorage, never()).store(any(), any());
    }

    @Test
    void create_withImage_storesUrl() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(postImageStorage.store(any(), any())).thenReturn("http://localhost:8080/media/posts/x.jpg");
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "photo.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );

        PostResponse response = postService.create(new CreatePostRequest("Hello"), image);

        verify(postImageStorage).store(any(UUID.class), eq(image));
        assertThat(response.imageUrl()).isEqualTo("http://localhost:8080/media/posts/x.jpg");
    }

    @Test
    void create_enrichesAuthorWhenUserServiceResponds() {
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(Map.of(
                authorId,
                new AuthorSummary(authorId, "tamara", "Tamara", "http://localhost:8080/media/profile-pictures/x.jpg")
        )).when(authorEnrichment).byIds(any());

        PostResponse response = postService.create(new CreatePostRequest("Hello"), null);

        assertThat(response.authorUsername()).isEqualTo("tamara");
        assertThat(response.authorDisplayName()).isEqualTo("Tamara");
        assertThat(response.authorProfilePictureUrl()).isEqualTo("http://localhost:8080/media/profile-pictures/x.jpg");
    }

    @Test
    void delete_author_deletesPostAndImage() {
        UUID postId = UUID.randomUUID();
        Post post = new Post(postId, authorId, "Hello");
        post.setImageUrl("http://localhost:8080/media/posts/" + postId + ".jpg");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.delete(postId);

        verify(postRepository).delete(post);
        verify(postImageStorage).delete(postId);
    }

    @Test
    void getById_includesLikeAndCommentCounts() {
        UUID postId = UUID.randomUUID();
        Post post = new Post(postId, authorId, "Hello");
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(likeRepository.countByPostId(postId)).thenReturn(3L);
        when(commentRepository.countByPostId(postId)).thenReturn(2L);

        PostResponse response = postService.getById(postId);

        assertThat(response.likeCount()).isEqualTo(3);
        assertThat(response.commentCount()).isEqualTo(2);
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
        verify(postImageStorage, never()).delete(any());
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
