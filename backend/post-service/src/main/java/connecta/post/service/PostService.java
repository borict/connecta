package connecta.post.service;

import connecta.post.domain.Post;
import connecta.post.dto.AuthorSummary;
import connecta.post.dto.CreatePostRequest;
import connecta.post.dto.PageResponse;
import connecta.post.dto.PostResponse;
import connecta.post.repository.CommentRepository;
import connecta.post.repository.PostLikeRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import connecta.post.security.SecurityUtils;
import connecta.post.storage.PostImageStorage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_AUTHOR_IDS = 100;

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final PostImageStorage postImageStorage;
    private final AuthorEnrichmentService authorEnrichment;
    private final PostVisibilityService visibility;

    public PostService(
            PostRepository postRepository,
            PostLikeRepository likeRepository,
            CommentRepository commentRepository,
            PostImageStorage postImageStorage,
            AuthorEnrichmentService authorEnrichment,
            PostVisibilityService visibility
    ) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.postImageStorage = postImageStorage;
        this.authorEnrichment = authorEnrichment;
        this.visibility = visibility;
    }

    @Transactional
    public PostResponse create(CreatePostRequest request, MultipartFile image) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must not be blank");
        }

        Post post = new Post(UUID.randomUUID(), currentUser.id(), content);
        if (image != null && !image.isEmpty()) {
            post.setImageUrl(postImageStorage.store(post.getId(), image));
        }
        return toResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public PostResponse getById(UUID postId) {
        return toResponse(requirePost(postId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listByUser(UUID userId, int page, int size) {
        visibility.requireVisibleProfilePosts(userId);
        PageRequest pageRequest = pageRequest(page, size);
        return toPageResponse(postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageRequest));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listByAuthors(String ids, int page, int size) {
        List<UUID> authorIds = parseAuthorIds(ids);
        PageRequest pageRequest = pageRequest(page, size);
        List<UUID> visibleAuthorIds = visibility.visibleAuthorIds(authorIds);
        if (visibleAuthorIds.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageRequest, 0));
        }
        return toPageResponse(postRepository.findByAuthorIdInOrderByCreatedAtDesc(visibleAuthorIds, pageRequest));
    }

    @Transactional
    public void delete(UUID postId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Post post = requirePost(postId);
        if (!post.getAuthorId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can delete this post");
        }
        postRepository.delete(post);
        postImageStorage.delete(post.getId());
    }

    private Post requirePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        visibility.requireVisiblePost(post);
        return post;
    }

    private PostResponse toResponse(Post post) {
        return toResponse(post, authorEnrichment.byIds(List.of(post.getAuthorId())));
    }

    private PageResponse<PostResponse> toPageResponse(org.springframework.data.domain.Page<Post> page) {
        Map<UUID, AuthorSummary> authors = authorEnrichment.byIds(
                page.getContent().stream().map(Post::getAuthorId).toList()
        );
        return PageResponse.from(page.map(post -> toResponse(post, authors)));
    }

    private PostResponse toResponse(Post post, Map<UUID, AuthorSummary> authors) {
        AuthorSummary author = authors.getOrDefault(post.getAuthorId(), AuthorSummary.fallback(post.getAuthorId()));
        return PostResponse.from(
                post,
                likeRepository.countByPostId(post.getId()),
                commentRepository.countByPostId(post.getId()),
                author
        );
    }

    static PageRequest pageRequest(int page, int size) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize);
    }

    static List<UUID> parseAuthorIds(String ids) {
        if (ids == null || ids.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids query parameter is required");
        }
        List<UUID> authorIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> {
                    try {
                        return UUID.fromString(value);
                    } catch (IllegalArgumentException ex) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id: " + value);
                    }
                })
                .distinct()
                .toList();
        if (authorIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids query parameter is required");
        }
        if (authorIds.size() > MAX_AUTHOR_IDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most 100 ids are allowed");
        }
        return authorIds;
    }
}
