package connecta.post.service;

import connecta.post.client.FollowStateDto;
import connecta.post.client.FollowingIdsDto;
import connecta.post.client.SocialClient;
import connecta.post.domain.Post;
import connecta.post.dto.AuthorSummary;
import connecta.post.security.AuthenticatedUser;
import connecta.post.security.SecurityUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostVisibilityService {

    private static final Logger log = LoggerFactory.getLogger(PostVisibilityService.class);

    private final AuthorEnrichmentService authorEnrichment;
    private final SocialClient socialClient;

    public PostVisibilityService(AuthorEnrichmentService authorEnrichment, SocialClient socialClient) {
        this.authorEnrichment = authorEnrichment;
        this.socialClient = socialClient;
    }

    public void requireVisiblePost(Post post) {
        if (!canSeeAuthor(post.getAuthorId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
    }

    public void requireVisibleProfilePosts(UUID authorId) {
        if (!canSeeAuthor(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This profile is private");
        }
    }

    public List<UUID> visibleAuthorIds(Collection<UUID> authorIds) {
        AuthenticatedUser viewer = SecurityUtils.requireCurrentUser();
        List<UUID> distinct = authorIds.stream().distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }

        Map<UUID, AuthorSummary> summaries = authorEnrichment.byIds(distinct);
        boolean needsFollowCheck = distinct.stream()
                .anyMatch(id -> needsFollowCheck(id, viewer, summaries));
        Set<UUID> following = needsFollowCheck ? acceptedFolloweeIds() : Set.of();

        List<UUID> visible = new ArrayList<>();
        for (UUID authorId : distinct) {
            if (authorId.equals(viewer.id())) {
                visible.add(authorId);
                continue;
            }
            AuthorSummary summary = summaries.get(authorId);
            if (summary != null && !summary.isPrivate()) {
                visible.add(authorId);
                continue;
            }
            if (following.contains(authorId)) {
                visible.add(authorId);
            }
        }
        return visible;
    }

    public boolean canSeeAuthor(UUID authorId) {
        AuthenticatedUser viewer = SecurityUtils.requireCurrentUser();
        if (authorId.equals(viewer.id())) {
            return true;
        }
        Map<UUID, AuthorSummary> summaries = authorEnrichment.byIds(List.of(authorId));
        AuthorSummary summary = summaries.get(authorId);
        if (summary != null && !summary.isPrivate()) {
            return true;
        }
        return isAcceptedFollower(authorId);
    }

    private static boolean needsFollowCheck(
            UUID authorId,
            AuthenticatedUser viewer,
            Map<UUID, AuthorSummary> summaries
    ) {
        if (authorId.equals(viewer.id())) {
            return false;
        }
        AuthorSummary summary = summaries.get(authorId);
        return summary == null || summary.isPrivate();
    }

    private boolean isAcceptedFollower(UUID authorId) {
        try {
            FollowStateDto state = socialClient.isFollowing(authorId);
            return state != null && state.following();
        } catch (RuntimeException ex) {
            log.warn("Social follow check failed for {}; hiding private content. cause={}", authorId, ex.toString());
            return false;
        }
    }

    private Set<UUID> acceptedFolloweeIds() {
        try {
            FollowingIdsDto dto = socialClient.followingIds();
            if (dto == null || dto.ids() == null) {
                return Set.of();
            }
            return new HashSet<>(dto.ids());
        } catch (RuntimeException ex) {
            log.warn("Social following ids failed; hiding private authors. cause={}", ex.toString());
            return Set.of();
        }
    }
}
