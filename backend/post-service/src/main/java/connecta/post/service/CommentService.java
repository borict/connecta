package connecta.post.service;

import connecta.post.domain.Comment;
import connecta.post.dto.AuthorSummary;
import connecta.post.dto.CommentResponse;
import connecta.post.dto.CreateCommentRequest;
import connecta.post.dto.PageResponse;
import connecta.post.repository.CommentRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import connecta.post.security.SecurityUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AuthorEnrichmentService authorEnrichment;

    public CommentService(
            PostRepository postRepository,
            CommentRepository commentRepository,
            AuthorEnrichmentService authorEnrichment
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.authorEnrichment = authorEnrichment;
    }

    @Transactional
    public CommentResponse create(UUID postId, CreateCommentRequest request) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        requirePost(postId);
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must not be blank");
        }
        Comment comment = new Comment(UUID.randomUUID(), postId, currentUser.id(), content);
        return toResponse(commentRepository.save(comment));
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

    private void requirePost(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
    }
}
