package connecta.message.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ConfigurationPropertiesBindingTest.TestConfig.class)
@TestPropertySource(properties = {
        "connecta.jwt.secret=test-secret-key-that-is-long-enough-for-hs256",
        "connecta.azure.servicebus.connection-string=",
        "connecta.azure.servicebus.topic=connecta-events"
})
class ConfigurationPropertiesBindingTest {

    @EnableConfigurationProperties({JwtProperties.class, ServiceBusProperties.class})
    static class TestConfig {
    }

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ServiceBusProperties serviceBusProperties;

    @Test
    void jwtAndServiceBusPropertiesBindFromConfiguration() {
        assertThat(jwtProperties.secret()).isEqualTo("test-secret-key-that-is-long-enough-for-hs256");
        assertThat(serviceBusProperties.topic()).isEqualTo("connecta-events");
        assertThat(serviceBusProperties.isConfigured()).isFalse();
    }
}
