package connecta.message.config;

import connecta.message.security.AuthHeaders;
import connecta.message.security.AuthenticatedUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
                return Optional.of(user.id());
            }

            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return Optional.empty();
            }
            String userId = attrs.getRequest().getHeader(AuthHeaders.USER_ID);
            if (userId == null || userId.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(UUID.fromString(userId));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        };
    }
}
