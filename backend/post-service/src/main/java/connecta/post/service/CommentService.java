package connecta.post.service;

import connecta.post.domain.Comment;
import connecta.post.domain.Post;
import connecta.post.dto.AuthorSummary;
import connecta.post.dto.CommentResponse;
import connecta.post.dto.CreateCommentRequest;
import connecta.post.dto.PageResponse;
import connecta.post.messaging.PostCommentedEvent;
import connecta.post.messaging.PostEventPublisher;
import connecta.post.repository.CommentRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import connecta.post.security.SecurityUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AuthorEnrichmentService authorEnrichment;
    private final PostEventPublisher eventPublisher;

    public CommentService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            AuthorEnrichmentService authorEnrichment,
            PostEventPublisher eventPublisher
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.authorEnrichment = authorEnrichment;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CommentResponse create(UUID postId, CreateCommentRequest request) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Post post = requirePost(postId);
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must not be blank");
        }
        Comment comment = new Comment(UUID.randomUUID(), postId, currentUser.id(), content);
        Comment saved = commentRepository.save(comment);
        publishCommented(post, saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(UUID postId, int page, int size) {
        requirePost(postId);
        var pageResult = commentRepository.findByPostIdOrderByCreatedAtDesc(
                postId,
                PostService.pageRequest(page, size)
        );
        Map<UUID, AuthorSummary> authors = authorEnrichment.byIds(
                pageResult.getContent().stream().map(Comment::getAuthorId).toList()
        );
        return PageResponse.from(pageResult.map(comment -> toResponse(comment, authors)));
    }

    @Transactional
    public void delete(UUID commentId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!comment.getAuthorId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can delete this comment");
        }
        commentRepository.delete(comment);
    }

    private void publishCommented(Post post, Comment comment) {
        if (post.getAuthorId().equals(comment.getAuthorId())) {
            return;
        }
        try {
            eventPublisher.publishPostCommented(PostCommentedEvent.of(
                    post.getId(),
                    post.getAuthorId(),
                    comment.getAuthorId(),
                    comment.getId(),
                    comment.getContent()
            ));
        } catch (RuntimeException ex) {
            log.warn("POST_COMMENTED publish failed; comment still succeeded. cause={}", ex.toString());
        }
    }

    private CommentResponse toResponse(Comment comment) {
        return toResponse(comment, authorEnrichment.byIds(List.of(comment.getAuthorId())));
    }

    private CommentResponse toResponse(Comment comment, Map<UUID, AuthorSummary> authors) {
        AuthorSummary author = authors.getOrDefault(
                comment.getAuthorId(),
                AuthorSummary.fallback(comment.getAuthorId())
        );
        return CommentResponse.from(comment, author);
    }

    private Post requirePost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }
}
