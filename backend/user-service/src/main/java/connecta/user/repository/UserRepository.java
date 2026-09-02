package connecta.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import connecta.user.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.username) = LOWER(:login)
               OR LOWER(u.email) = LOWER(:login)
            """)
    Optional<User> findByUsernameOrEmailIgnoreCase(@Param("login") String login);

    @Query("""
            SELECT u FROM User u
            WHERE u.isActive = true
              AND (
                    LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<User> searchByUsernameOrDisplayName(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT u.id FROM User u
            WHERE u.isActive = true
              AND u.isBanned = false
              AND u.isPrivate = false
              AND u.id <> :excludeId
            ORDER BY u.createdAt DESC
            """)
    List<UUID> findPublicIdsExcluding(@Param("excludeId") UUID excludeId, Pageable pageable);

    List<User> findByIdInAndIsActiveTrue(Collection<UUID> ids);

    Optional<User> findByIdAndIsActiveTrue(UUID id);

    Optional<User> findByUsernameIgnoreCaseAndIsActiveTrue(String username);
}
