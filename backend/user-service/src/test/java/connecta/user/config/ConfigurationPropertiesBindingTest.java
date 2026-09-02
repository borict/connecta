package connecta.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ConfigurationPropertiesBindingTest.TestConfig.class)
@TestPropertySource(properties = {
        "connecta.jwt.secret=test-secret-key-that-is-long-enough-for-hs256",
        "connecta.jwt.expiration-ms=3600000",
        "connecta.storage.local.base-dir=./uploads-test",
        "connecta.storage.local.public-base-url=http://localhost:8081/media",
        "connecta.storage.azure.connection-string=",
        "connecta.storage.azure.container-avatars=avatars"
})
class ConfigurationPropertiesBindingTest {

    @EnableConfigurationProperties({JwtProperties.class, StorageProperties.class, AzureStorageProperties.class})
    static class TestConfig {
    }

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    private AzureStorageProperties azureStorageProperties;

    @Test
    void jwtAndStoragePropertiesBindFromConfiguration() {
        assertThat(jwtProperties.secret()).isEqualTo("test-secret-key-that-is-long-enough-for-hs256");
        assertThat(jwtProperties.expirationMs()).isEqualTo(3_600_000L);
        assertThat(storageProperties.baseDir()).isEqualTo("./uploads-test");
        assertThat(storageProperties.publicBaseUrl()).isEqualTo("http://localhost:8081/media");
        assertThat(azureStorageProperties.isConfigured()).isFalse();
        assertThat(azureStorageProperties.containerAvatars()).isEqualTo("avatars");
    }
}
