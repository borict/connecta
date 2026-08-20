package connecta.post.repository;

import connecta.post.domain.PostLike;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    long countByPostId(UUID postId);

    long deleteByPostIdAndUserId(UUID postId, UUID userId);
}
