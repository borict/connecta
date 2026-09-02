package connecta.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connecta.storage.azure")
public record AzureStorageProperties(
        String connectionString,
        String containerPosts
) {

    public AzureStorageProperties {
        if (containerPosts == null || containerPosts.isBlank()) {
            containerPosts = "posts";
        }
    }

    public boolean isConfigured() {
        return connectionString != null && !connectionString.isBlank();
    }
}
