package connecta.post.repository;

import connecta.post.domain.Comment;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByPostIdOrderByCreatedAtDesc(UUID postId, Pageable pageable);

    long countByPostId(UUID postId);
}
