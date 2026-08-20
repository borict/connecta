package connecta.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.post.domain.Comment;
import connecta.post.domain.Post;
import connecta.post.domain.Role;
import connecta.post.dto.AuthorSummary;
import connecta.post.dto.CommentResponse;
import connecta.post.dto.CreateCommentRequest;
import connecta.post.messaging.PostCommentedEvent;
import connecta.post.messaging.PostEventPublisher;
import connecta.post.repository.CommentRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AuthorEnrichmentService authorEnrichment;

    @Mock
    private PostEventPublisher eventPublisher;

    private CommentService commentService;
    private UUID userId;
    private UUID postId;
    private Post otherUsersPost;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(postRepository, commentRepository, authorEnrichment, eventPublisher);
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        otherUsersPost = new Post(postId, UUID.randomUUID(), "Hello");
        AuthenticatedUser user = new AuthenticatedUser(userId, "tamara", Role.USER);
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
    void create_missingPost_returnsNotFound() {
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(postId, new CreateCommentRequest("Nice")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void create_persistsTrimmedContent() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.create(postId, new CreateCommentRequest("  Nice post!  "));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        Comment saved = captor.getValue();
        assertThat(saved.getPostId()).isEqualTo(postId);
        assertThat(saved.getAuthorId()).isEqualTo(userId);
        assertThat(saved.getContent()).isEqualTo("Nice post!");
        verify(eventPublisher).publishPostCommented(any(PostCommentedEvent.class));
        assertThat(response.content()).isEqualTo("Nice post!");
        assertThat(response.authorId()).isEqualTo(userId);
        assertThat(response.authorUsername()).isNull();
    }

    @Test
    void create_enrichesAuthorWhenUserServiceResponds() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(Map.of(
                userId,
                new AuthorSummary(userId, "tamara", "Tamara", null)
        )).when(authorEnrichment).byIds(any());

        CommentResponse response = commentService.create(postId, new CreateCommentRequest("Nice"));

        assertThat(response.authorUsername()).isEqualTo("tamara");
        assertThat(response.authorDisplayName()).isEqualTo("Tamara");
    }

    @Test
    void create_ownPost_doesNotPublishEvent() {
        Post ownPost = new Post(postId, userId, "Hello");
        when(postRepository.findById(postId)).thenReturn(Optional.of(ownPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.create(postId, new CreateCommentRequest("Note to self"));

        verify(eventPublisher, never()).publishPostCommented(any());
    }

    @Test
    void create_publisherFails_commentStillSucceeds() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(otherUsersPost));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("bus down")).when(eventPublisher).publishPostCommented(any());

        CommentResponse response = commentService.create(postId, new CreateCommentRequest("Nice"));

        assertThat(response.content()).isEqualTo("Nice");
    }

    @Test
    void delete_author_deletesComment() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment(commentId, postId, userId, "Nice");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        commentService.delete(commentId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void delete_nonAuthor_returnsForbidden() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment(commentId, postId, UUID.randomUUID(), "Nice");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(commentId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(statusEx.getReason()).isEqualTo("Only the author can delete this comment");
                });
        verify(commentRepository, never()).delete(any());
    }
}
