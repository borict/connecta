package connecta.post.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AzureStoragePropertiesTest {

    @Test
    void blankConnectionStringIsNotConfigured() {
        assertThat(new AzureStorageProperties(null, "posts").isConfigured()).isFalse();
        assertThat(new AzureStorageProperties("  ", "posts").isConfigured()).isFalse();
        assertThat(new AzureStorageProperties(
                "DefaultEndpointsProtocol=https;AccountName=connecta;AccountKey=abc;EndpointSuffix=core.windows.net",
                "posts"
        ).isConfigured()).isTrue();
    }

    @Test
    void blankContainerDefaultsToPosts() {
        assertThat(new AzureStorageProperties("conn", " ").containerPosts()).isEqualTo("posts");
    }
}
