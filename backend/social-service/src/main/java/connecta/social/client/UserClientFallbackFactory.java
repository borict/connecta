package connecta.social.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    private static final Logger log = LoggerFactory.getLogger(UserClientFallbackFactory.class);

    @Override
    public UserClient create(Throwable cause) {
        return ids -> {
            log.warn("User Service unavailable; cannot resolve follow target. cause={}", cause.toString());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "User Service unavailable");
        };
    }
}
