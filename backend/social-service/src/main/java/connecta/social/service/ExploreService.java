package connecta.social.service;

import connecta.social.client.FeedPostDto;
import connecta.social.client.PostClient;
import connecta.social.client.PublicIdsDto;
import connecta.social.client.UserClient;
import connecta.social.domain.Follow;
import connecta.social.domain.FollowStatus;
import connecta.social.dto.PageResponse;
import connecta.social.repository.FollowRepository;
import connecta.social.security.AuthenticatedUser;
import connecta.social.security.SecurityUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExploreService {

    private static final Logger log = LoggerFactory.getLogger(ExploreService.class);

    private final UserClient userClient;
    private final FollowRepository followRepository;
    private final PostClient postClient;

    public ExploreService(UserClient userClient, FollowRepository followRepository, PostClient postClient) {
        this.userClient = userClient;
        this.followRepository = followRepository;
        this.postClient = postClient;
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedPostDto> explore(int page, int size) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        var pageRequest = FollowService.pageRequest(page, size);
        int safePage = pageRequest.getPageNumber();
        int safeSize = pageRequest.getPageSize();

        List<UUID> publicIds;
        try {
            PublicIdsDto dto = userClient.publicIds();
            publicIds = dto == null || dto.ids() == null ? List.of() : dto.ids();
        } catch (RuntimeException ex) {
            log.warn("User Service public-ids failed; returning empty explore. cause={}", ex.toString());
            return PageResponse.empty(safePage, safeSize);
        }

        Set<UUID> following = followRepository.findByFollowerIdAndStatus(currentUser.id(), FollowStatus.ACCEPTED)
                .stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toCollection(HashSet::new));

        List<UUID> authorIds = publicIds.stream()
                .filter(id -> id != null && !id.equals(currentUser.id()) && !following.contains(id))
                .distinct()
                .limit(FeedService.MAX_AUTHOR_IDS)
                .toList();

        if (authorIds.isEmpty()) {
            return PageResponse.empty(safePage, safeSize);
        }

        String ids = authorIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        try {
            PageResponse<FeedPostDto> result = postClient.listByAuthors(ids, safePage, safeSize);
            if (result == null) {
                return PageResponse.empty(safePage, safeSize);
            }
            return result;
        } catch (RuntimeException ex) {
            log.warn("Post Service explore call failed; returning empty explore. cause={}", ex.toString());
            return PageResponse.empty(safePage, safeSize);
        }
    }
}
