package connecta.post.service;

import connecta.post.client.UserClient;
import connecta.post.client.UserSummaryDto;
import connecta.post.dto.AuthorSummary;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuthorEnrichmentService {

    static final int MAX_BATCH_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(AuthorEnrichmentService.class);

    private final UserClient userClient;

    public AuthorEnrichmentService(UserClient userClient) {
        this.userClient = userClient;
    }

    public Map<UUID, AuthorSummary> byIds(Collection<UUID> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinct = authorIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }

        Map<UUID, AuthorSummary> result = new HashMap<>();
        for (int i = 0; i < distinct.size(); i += MAX_BATCH_SIZE) {
            List<UUID> chunk = distinct.subList(i, Math.min(i + MAX_BATCH_SIZE, distinct.size()));
            String ids = chunk.stream().map(UUID::toString).collect(Collectors.joining(","));
            try {
                List<UserSummaryDto> users = userClient.batchUsers(ids);
                if (users == null) {
                    continue;
                }
                for (UserSummaryDto user : users) {
                    if (user == null || user.id() == null) {
                        continue;
                    }
                    result.put(
                            user.id(),
                            new AuthorSummary(
                                    user.id(),
                                    user.username(),
                                    user.displayName(),
                                    user.profilePictureUrl()
                            )
                    );
                }
            } catch (RuntimeException ex) {
                log.warn("User Service enrichment failed for {} ids: {}", chunk.size(), ex.toString());
            }
        }
        return result;
    }
}
