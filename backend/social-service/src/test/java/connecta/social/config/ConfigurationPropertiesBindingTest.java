package connecta.social.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ConfigurationPropertiesBindingTest.TestConfig.class)
@TestPropertySource(properties = {
        "connecta.jwt.secret=test-secret-key-that-is-long-enough-for-hs256"
})
class ConfigurationPropertiesBindingTest {

    @EnableConfigurationProperties(JwtProperties.class)
    static class TestConfig {
    }

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void jwtPropertiesBindFromConfiguration() {
        assertThat(jwtProperties.secret()).isEqualTo("test-secret-key-that-is-long-enough-for-hs256");
    }
}
