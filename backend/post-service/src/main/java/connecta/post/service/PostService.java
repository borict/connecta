package connecta.post.service;

import connecta.post.domain.Post;
import connecta.post.dto.CreatePostRequest;
import connecta.post.dto.PageResponse;
import connecta.post.dto.PostResponse;
import connecta.post.repository.PostLikeRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import connecta.post.security.SecurityUtils;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;
    static final int MAX_AUTHOR_IDS = 100;

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;

    public PostService(PostRepository postRepository, PostLikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
    }

    @Transactional
    public PostResponse create(CreatePostRequest request) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must not be blank");
        }

        Post post = new Post(UUID.randomUUID(), currentUser.id(), content);
        return toResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public PostResponse getById(UUID postId) {
        return toResponse(requirePost(postId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listByUser(UUID userId, int page, int size) {
        PageRequest pageRequest = pageRequest(page, size);
        return PageResponse.from(
                postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageRequest)
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listByAuthors(String ids, int page, int size) {
        List<UUID> authorIds = parseAuthorIds(ids);
        PageRequest pageRequest = pageRequest(page, size);
        return PageResponse.from(
                postRepository.findByAuthorIdInOrderByCreatedAtDesc(authorIds, pageRequest)
                        .map(this::toResponse)
        );
    }

    @Transactional
    public void delete(UUID postId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Post post = requirePost(postId);
        if (!post.getAuthorId().equals(currentUser.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can delete this post");
        }
        postRepository.delete(post);
    }

    private Post requirePost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private PostResponse toResponse(Post post) {
        return PostResponse.from(post, likeRepository.countByPostId(post.getId()), 0L);
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
