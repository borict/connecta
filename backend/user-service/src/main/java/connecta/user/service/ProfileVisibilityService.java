package connecta.user.service;

import connecta.user.client.FollowStateDto;
import connecta.user.client.SocialClient;
import connecta.user.domain.User;
import connecta.user.security.AuthenticatedUser;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProfileVisibilityService {

    private static final Logger log = LoggerFactory.getLogger(ProfileVisibilityService.class);

    private final SocialClient socialClient;

    public ProfileVisibilityService(SocialClient socialClient) {
        this.socialClient = socialClient;
    }

    public boolean canSeeFullProfile(User target, AuthenticatedUser viewer) {
        if (target.getId().equals(viewer.id()) || !target.isPrivate()) {
            return true;
        }
        return isAcceptedFollower(target.getId());
    }

    private boolean isAcceptedFollower(UUID userId) {
        try {
            FollowStateDto state = socialClient.isFollowing(userId);
            return state != null && state.following();
        } catch (RuntimeException ex) {
            log.warn("Social follow check failed for {}; returning limited profile. cause={}", userId, ex.toString());
            return false;
        }
    }
}
