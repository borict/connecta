package connecta.social.service;

import connecta.social.client.UserClient;
import connecta.social.client.UserSummaryDto;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserLookupService {

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
}
