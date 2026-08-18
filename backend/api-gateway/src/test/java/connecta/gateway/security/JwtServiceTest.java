package connecta.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import connecta.gateway.config.JwtProperties;
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
    void parse_validToken_returnsPrincipal() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim(JwtClaims.USERNAME, "tamara")
                .claim(JwtClaims.ROLE, "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();

        assertThat(jwtService.parse(token))
                .hasValueSatisfying(principal -> {
                    assertThat(principal.userId()).isEqualTo(userId.toString());
                    assertThat(principal.username()).isEqualTo("tamara");
                    assertThat(principal.role()).isEqualTo("USER");
                });
    }

    @Test
    void parse_invalidToken_returnsEmpty() {
        assertThat(jwtService.parse("not-a-token")).isEmpty();
    }
}
