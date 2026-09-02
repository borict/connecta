package connecta.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connecta.storage.azure")
public record AzureStorageProperties(
        String connectionString,
        String containerAvatars
) {

    public AzureStorageProperties {
        if (containerAvatars == null || containerAvatars.isBlank()) {
            containerAvatars = "avatars";
        }
    }

    public boolean isConfigured() {
        return connectionString != null && !connectionString.isBlank();
    }
}
