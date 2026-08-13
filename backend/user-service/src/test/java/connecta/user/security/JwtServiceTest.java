package connecta.user.security;

import static org.assertj.core.api.Assertions.assertThat;

import connecta.user.config.JwtProperties;
import connecta.user.domain.Role;
import connecta.user.domain.User;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(
                "test-secret-key-that-is-long-enough-for-hs256",
                3_600_000L
        ));
    }

    @Test
    void createToken_containsExpectedClaims() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "tamara",
                "tamara@example.com",
                "hash",
                "Tamara",
                LocalDate.of(2000, 1, 1)
        );
        user.setRole(Role.USER);

        String token = jwtService.createToken(user);

        assertThat(jwtService.parseUserId(token)).isEqualTo(userId);
        assertThat(jwtService.parseUsername(token)).isEqualTo("tamara");
        assertThat(jwtService.parseRole(token)).isEqualTo(Role.USER);
    }
}
