package connecta.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AzureStoragePropertiesTest {

    @Test
    void blankConnectionStringIsNotConfigured() {
        assertThat(new AzureStorageProperties(null, "avatars").isConfigured()).isFalse();
        assertThat(new AzureStorageProperties("  ", "avatars").isConfigured()).isFalse();
        assertThat(new AzureStorageProperties(
                "DefaultEndpointsProtocol=https;AccountName=connecta;AccountKey=abc;EndpointSuffix=core.windows.net",
                "avatars"
        ).isConfigured()).isTrue();
    }

    @Test
    void blankContainerDefaultsToAvatars() {
        assertThat(new AzureStorageProperties("conn", " ").containerAvatars()).isEqualTo("avatars");
    }
}
