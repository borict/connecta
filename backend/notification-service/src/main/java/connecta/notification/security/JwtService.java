package connecta.notification.security;

import connecta.notification.config.JwtProperties;
import connecta.notification.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public Optional<AuthenticatedUser> parseAuthenticatedUser(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get(JwtClaims.USERNAME, String.class);
            String roleClaim = claims.get(JwtClaims.ROLE, String.class);
            if (username == null || username.isBlank() || roleClaim == null || roleClaim.isBlank()) {
                return Optional.empty();
            }
            Role role = Role.valueOf(roleClaim);
            return Optional.of(new AuthenticatedUser(userId, username, role));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
