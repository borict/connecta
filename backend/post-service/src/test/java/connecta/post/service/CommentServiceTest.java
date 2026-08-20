package connecta.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.post.domain.Comment;
import connecta.post.domain.Role;
import connecta.post.dto.CommentResponse;
import connecta.post.dto.CreateCommentRequest;
import connecta.post.repository.CommentRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
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

    private CommentService commentService;
    private UUID userId;
    private UUID postId;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(postRepository, commentRepository);
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
    void create_missingPost_returnsNotFound() {
        when(postRepository.existsById(postId)).thenReturn(false);

        assertThatThrownBy(() -> commentService.create(postId, new CreateCommentRequest("Nice")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void create_persistsTrimmedContent() {
        when(postRepository.existsById(postId)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.create(postId, new CreateCommentRequest("  Nice post!  "));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        Comment saved = captor.getValue();
        assertThat(saved.getPostId()).isEqualTo(postId);
        assertThat(saved.getAuthorId()).isEqualTo(userId);
        assertThat(saved.getContent()).isEqualTo("Nice post!");
        assertThat(response.content()).isEqualTo("Nice post!");
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
