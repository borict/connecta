package connecta.social.repository;

import connecta.social.domain.Follow;
import connecta.social.domain.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
}
