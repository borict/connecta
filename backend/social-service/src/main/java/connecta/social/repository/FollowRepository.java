package connecta.social.repository;

import connecta.social.domain.Follow;
import connecta.social.domain.FollowId;
import connecta.social.domain.FollowStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    Page<Follow> findByFolloweeIdAndStatusOrderByCreatedAtDesc(
            UUID followeeId,
            FollowStatus status,
            Pageable pageable
    );
}
