package connecta.post.service;

import connecta.post.domain.PostLike;
import connecta.post.dto.LikeCountResponse;
import connecta.post.dto.LikeResponse;
import connecta.post.dto.LikedResponse;
import connecta.post.repository.PostLikeRepository;
import connecta.post.repository.PostRepository;
import connecta.post.security.AuthenticatedUser;
import connecta.post.security.SecurityUtils;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;

    public LikeService(PostRepository postRepository, PostLikeRepository likeRepository) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
    }

    @Transactional
    public LikeResponse like(UUID postId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        requirePost(postId);
        if (!likeRepository.existsByPostIdAndUserId(postId, currentUser.id())) {
            try {
                likeRepository.save(new PostLike(UUID.randomUUID(), postId, currentUser.id()));
            } catch (DataIntegrityViolationException ignored) {
                // Concurrent duplicate like — unique (post_id, user_id) already holds.
            }
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

    private void requirePost(UUID postId) {
        if (!postRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
    }
}
