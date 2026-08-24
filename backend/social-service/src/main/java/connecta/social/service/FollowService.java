package connecta.social.service;

import connecta.social.client.UserSummaryDto;
import connecta.social.domain.Follow;
import connecta.social.domain.FollowId;
import connecta.social.domain.FollowStatus;
import connecta.social.dto.FollowResponse;
import connecta.social.repository.FollowRepository;
import connecta.social.security.AuthenticatedUser;
import connecta.social.security.SecurityUtils;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FollowService {

    public record FollowResult(FollowResponse follow, boolean created) {
    }

    private final FollowRepository followRepository;
    private final UserLookupService userLookup;

    public FollowService(FollowRepository followRepository, UserLookupService userLookup) {
        this.followRepository = followRepository;
        this.userLookup = userLookup;
    }

    @Transactional
    public FollowResult follow(UUID followeeId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        if (currentUser.id().equals(followeeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
        }

        FollowId id = new FollowId(currentUser.id(), followeeId);
        Follow existing = followRepository.findById(id).orElse(null);
        if (existing != null) {
            return new FollowResult(FollowResponse.from(existing), false);
        }

        UserSummaryDto target = userLookup.requireActiveUser(followeeId);
        FollowStatus status = target.isPrivate() ? FollowStatus.PENDING : FollowStatus.ACCEPTED;
        Follow created = new Follow(currentUser.id(), followeeId, status);
        try {
            return new FollowResult(FollowResponse.from(followRepository.save(created)), true);
        } catch (DataIntegrityViolationException ex) {
            Follow raced = followRepository.findById(id)
                    .orElseThrow(() -> ex);
            return new FollowResult(FollowResponse.from(raced), false);
        }
    }

    @Transactional
    public void unfollow(UUID followeeId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        if (currentUser.id().equals(followeeId)) {
            return;
        }
        followRepository.findById(new FollowId(currentUser.id(), followeeId))
                .ifPresent(followRepository::delete);
    }
}
