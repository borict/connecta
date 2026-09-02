package connecta.post.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ConfigurationPropertiesBindingTest.TestConfig.class)
@TestPropertySource(properties = {
        "connecta.jwt.secret=test-secret-key-that-is-long-enough-for-hs256",
        "connecta.storage.local.base-dir=./uploads-test",
        "connecta.storage.local.public-base-url=http://localhost:8080/media",
        "connecta.storage.azure.connection-string=",
        "connecta.storage.azure.container-posts=posts"
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
        assertThat(storageProperties.baseDir()).isEqualTo("./uploads-test");
        assertThat(storageProperties.publicBaseUrl()).isEqualTo("http://localhost:8080/media");
        assertThat(azureStorageProperties.isConfigured()).isFalse();
        assertThat(azureStorageProperties.containerPosts()).isEqualTo("posts");
    }
}
