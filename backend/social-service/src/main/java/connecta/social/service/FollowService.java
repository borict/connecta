package connecta.social.service;

import connecta.social.client.UserSummaryDto;
import connecta.social.domain.Follow;
import connecta.social.domain.FollowId;
import connecta.social.domain.FollowStatus;
import connecta.social.dto.FollowResponse;
import connecta.social.dto.FollowStateResponse;
import connecta.social.dto.FollowStatsResponse;
import connecta.social.dto.FollowUserResponse;
import connecta.social.dto.FollowingIdsResponse;
import connecta.social.dto.PageResponse;
import connecta.social.repository.FollowRepository;
import connecta.social.security.AuthenticatedUser;
import connecta.social.security.SecurityUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FollowService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;

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

    @Transactional
    public FollowResponse accept(UUID followerId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Follow follow = requireIncomingRequest(followerId, currentUser.id());
        if (follow.getStatus() == FollowStatus.ACCEPTED) {
            return FollowResponse.from(follow);
        }
        follow.setStatus(FollowStatus.ACCEPTED);
        return FollowResponse.from(followRepository.save(follow));
    }

    @Transactional
    public void reject(UUID followerId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Follow follow = followRepository.findById(new FollowId(followerId, currentUser.id())).orElse(null);
        if (follow == null) {
            return;
        }
        if (follow.getStatus() != FollowStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Follow is not pending");
        }
        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> incomingRequests(int page, int size) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        Page<Follow> follows = followRepository.findByFolloweeIdAndStatusOrderByCreatedAtDesc(
                currentUser.id(),
                FollowStatus.PENDING,
                pageRequest(page, size)
        );
        return toUserPage(follows, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> followers(UUID userId, int page, int size) {
        Page<Follow> follows = followRepository.findByFolloweeIdAndStatusOrderByCreatedAtDesc(
                userId,
                FollowStatus.ACCEPTED,
                pageRequest(page, size)
        );
        return toUserPage(follows, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> following(UUID userId, int page, int size) {
        Page<Follow> follows = followRepository.findByFollowerIdAndStatusOrderByCreatedAtDesc(
                userId,
                FollowStatus.ACCEPTED,
                pageRequest(page, size)
        );
        return toUserPage(follows, false);
    }

    @Transactional(readOnly = true)
    public FollowStatsResponse stats(UUID userId) {
        return new FollowStatsResponse(
                followRepository.countByFolloweeIdAndStatus(userId, FollowStatus.ACCEPTED),
                followRepository.countByFollowerIdAndStatus(userId, FollowStatus.ACCEPTED)
        );
    }

    @Transactional(readOnly = true)
    public FollowingIdsResponse followingIds() {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        List<UUID> ids = followRepository.findByFollowerIdAndStatus(currentUser.id(), FollowStatus.ACCEPTED)
                .stream()
                .map(Follow::getFolloweeId)
                .toList();
        return new FollowingIdsResponse(ids);
    }

    @Transactional(readOnly = true)
    public FollowStateResponse isFollowing(UUID userId) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        if (currentUser.id().equals(userId)) {
            return new FollowStateResponse(false, false);
        }
        Follow follow = followRepository.findById(new FollowId(currentUser.id(), userId)).orElse(null);
        if (follow == null) {
            return new FollowStateResponse(false, false);
        }
        return new FollowStateResponse(
                follow.getStatus() == FollowStatus.ACCEPTED,
                follow.getStatus() == FollowStatus.PENDING
        );
    }

    private PageResponse<FollowUserResponse> toUserPage(Page<Follow> follows, boolean listFollowers) {
        List<UUID> ids = follows.getContent().stream()
                .map(follow -> listFollowers ? follow.getFollowerId() : follow.getFolloweeId())
                .toList();
        Map<UUID, UserSummaryDto> users = userLookup.summariesByIds(ids);
        return PageResponse.from(follows.map(follow -> toUserResponse(follow, listFollowers, users)));
    }

    private FollowUserResponse toUserResponse(
            Follow follow,
            boolean listFollowers,
            Map<UUID, UserSummaryDto> users
    ) {
        UUID userId = listFollowers ? follow.getFollowerId() : follow.getFolloweeId();
        UserSummaryDto user = users.get(userId);
        return new FollowUserResponse(
                userId,
                user != null ? user.username() : null,
                user != null ? user.displayName() : null,
                user != null ? user.profilePictureUrl() : null,
                follow.getCreatedAt()
        );
    }

    private Follow requireIncomingRequest(UUID followerId, UUID followeeId) {
        return followRepository.findById(new FollowId(followerId, followeeId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follow request not found"));
    }

    static PageRequest pageRequest(int page, int size) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize);
    }
}
