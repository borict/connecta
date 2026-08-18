package connecta.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminSeedPasswordTest {

    @Test
    void seedAdminPasswordHashMatchesDocumentedPassword() {
        String hash = "$2a$10$6GTanUou8QLx.ne0Z0wuC.qaWegvp5XykjnebC//1a38JJP542iDe";
        assertThat(new BCryptPasswordEncoder().matches("Admin123!", hash)).isTrue();
    }
}
