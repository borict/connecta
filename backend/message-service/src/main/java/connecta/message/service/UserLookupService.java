package connecta.message.service;

import connecta.message.client.UserClient;
import connecta.message.client.UserSummaryDto;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserLookupService {

    static final int MAX_BATCH_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(UserLookupService.class);

    private final UserClient userClient;

    public UserLookupService(UserClient userClient) {
        this.userClient = userClient;
    }

    public UserSummaryDto requireActiveUser(UUID userId) {
        List<UserSummaryDto> users;
        try {
            users = userClient.batchUsers(userId.toString());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("User Service lookup failed for {}: {}", userId, ex.toString());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User Service unavailable");
        }
        if (users == null || users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return users.stream()
                .filter(user -> user != null && userId.equals(user.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Map<UUID, UserSummaryDto> summariesByIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinct = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UserSummaryDto> result = new HashMap<>();
        for (int i = 0; i < distinct.size(); i += MAX_BATCH_SIZE) {
            List<UUID> chunk = distinct.subList(i, Math.min(i + MAX_BATCH_SIZE, distinct.size()));
            String ids = chunk.stream().map(UUID::toString).collect(Collectors.joining(","));
            try {
                List<UserSummaryDto> users = userClient.batchUsers(ids);
                if (users == null) {
                    continue;
                }
                for (UserSummaryDto user : users) {
                    if (user != null && user.id() != null) {
                        result.put(user.id(), user);
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("User Service enrichment failed for {} ids: {}", chunk.size(), ex.toString());
            }
        }
        return result;
    }
}
