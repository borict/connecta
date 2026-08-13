package connecta.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "connecta.storage.local")
public record StorageProperties(
        String baseDir,
        String publicBaseUrl
) {
}
