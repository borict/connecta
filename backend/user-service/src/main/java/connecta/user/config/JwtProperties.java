package connecta.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connecta.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
}
