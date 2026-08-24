package connecta.social.service;

import connecta.social.client.FeedPostDto;
import connecta.social.client.PostClient;
import connecta.social.domain.Follow;
import connecta.social.domain.FollowStatus;
import connecta.social.dto.PageResponse;
import connecta.social.repository.FollowRepository;
import connecta.social.security.AuthenticatedUser;
import connecta.social.security.SecurityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {

    static final int MAX_AUTHOR_IDS = 100;

    private static final Logger log = LoggerFactory.getLogger(FeedService.class);

    private final FollowRepository followRepository;
    private final PostClient postClient;

    public FeedService(FollowRepository followRepository, PostClient postClient) {
        this.followRepository = followRepository;
        this.postClient = postClient;
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedPostDto> feed(int page, int size) {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        var pageRequest = FollowService.pageRequest(page, size);
        int safePage = pageRequest.getPageNumber();
        int safeSize = pageRequest.getPageSize();

        List<UUID> authorIds = new ArrayList<>();
        authorIds.add(currentUser.id());
        followRepository.findByFollowerIdAndStatus(currentUser.id(), FollowStatus.ACCEPTED)
                .stream()
                .map(Follow::getFolloweeId)
                .filter(id -> !id.equals(currentUser.id()))
                .distinct()
                .limit(MAX_AUTHOR_IDS - 1L)
                .forEach(authorIds::add);

        String ids = authorIds.stream().map(UUID::toString).collect(Collectors.joining(","));
        try {
            PageResponse<FeedPostDto> result = postClient.listByAuthors(ids, safePage, safeSize);
            if (result == null) {
                return PageResponse.empty(safePage, safeSize);
            }
            return result;
        } catch (RuntimeException ex) {
            log.warn("Post Service feed call failed; returning empty feed. cause={}", ex.toString());
            return PageResponse.empty(safePage, safeSize);
        }
    }
}
