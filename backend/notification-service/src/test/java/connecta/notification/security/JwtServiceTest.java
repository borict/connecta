package connecta.notification.security;

import static org.assertj.core.api.Assertions.assertThat;

import connecta.notification.config.JwtProperties;
import connecta.notification.domain.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256";

    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET));
        secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parseAuthenticatedUser_validToken_returnsUser() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim(JwtClaims.USERNAME, "tamara")
                .claim(JwtClaims.ROLE, "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();

        assertThat(jwtService.parseAuthenticatedUser(token))
                .hasValueSatisfying(user -> {
                    assertThat(user.id()).isEqualTo(userId);
                    assertThat(user.username()).isEqualTo("tamara");
                    assertThat(user.role()).isEqualTo(Role.USER);
                });
    }

    @Test
    void parseAuthenticatedUser_invalidToken_returnsEmpty() {
        assertThat(jwtService.parseAuthenticatedUser("not-a-token")).isEmpty();
    }

    @Test
    void parseAuthenticatedUser_missingClaims_returnsEmpty() {
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();

        assertThat(jwtService.parseAuthenticatedUser(token)).isEmpty();
    }
}
