package connecta.post.service;

import connecta.post.domain.Post;
import connecta.post.domain.PostLike;
import connecta.post.dto.LikeCountResponse;
import connecta.post.dto.LikeResponse;
import connecta.post.dto.LikedResponse;
import connecta.post.messaging.PostEventPublisher;
import connecta.post.messaging.PostLikedEvent;
import connecta.post.repository.PostLikeRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import connecta.post.security.SecurityUtils;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LikeService {

    private static final Logger log = LoggerFactory.getLogger(LikeService.class);

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final PostEventPublisher eventPublisher;
    private final PostVisibilityService visibility;

    public LikeService(
            PostRepository postRepository,
            PostLikeRepository likeRepository,
            PostEventPublisher eventPublisher,
            PostVisibilityService visibility
    ) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.eventPublisher = eventPublisher;
        this.visibility = visibility;
    }

    @Transactional
    public LikeResponse like(UUID postId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Post post = requirePost(postId);
        boolean created = false;
        if (!likeRepository.existsByPostIdAndUserId(postId, currentUser.id())) {
            try {
                likeRepository.save(new PostLike(UUID.randomUUID(), postId, currentUser.id()));
                created = true;
            } catch (DataIntegrityViolationException ignored) {
                // Concurrent duplicate like — unique (post_id, user_id) already holds.
            }
        }
        if (created) {
            publishLiked(post, currentUser.id());
        }
        return new LikeResponse(true, likeRepository.countByPostId(postId));
    }

    @Transactional
    public void unlike(UUID postId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        requirePost(postId);
        likeRepository.deleteByPostIdAndUserId(postId, currentUser.id());
    }

    @Transactional(readOnly = true)
    public LikeCountResponse count(UUID postId) {
        requirePost(postId);
        return new LikeCountResponse(likeRepository.countByPostId(postId));
    }

    @Transactional(readOnly = true)
    public LikedResponse liked(UUID postId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        requirePost(postId);
        return new LikedResponse(likeRepository.existsByPostIdAndUserId(postId, currentUser.id()));
    }

    private void publishLiked(Post post, UUID actorId) {
        if (post.getAuthorId().equals(actorId)) {
            return;
        }
        try {
            eventPublisher.publishPostLiked(PostLikedEvent.of(post.getId(), post.getAuthorId(), actorId));
        } catch (RuntimeException ex) {
            log.warn("POST_LIKED publish failed; like still succeeded. cause={}", ex.toString());
        }
    }

    private Post requirePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        visibility.requireVisiblePost(post);
        return post;
    }
}
