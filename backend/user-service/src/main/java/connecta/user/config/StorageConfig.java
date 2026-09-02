package connecta.user.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import connecta.user.storage.AzureProfilePictureStorage;
import connecta.user.storage.LocalProfilePictureStorage;
import connecta.user.storage.ProfilePictureStorage;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    ProfilePictureStorage profilePictureStorage(
            AzureStorageProperties azure,
            StorageProperties local
    ) throws IOException {
        if (azure.isConfigured()) {
            try {
                BlobContainerClient container = new BlobServiceClientBuilder()
                        .connectionString(azure.connectionString())
                        .buildClient()
                        .getBlobContainerClient(azure.containerAvatars());
                container.createIfNotExists();
                log.info("Azure Blob storage enabled for profile pictures; container={}", azure.containerAvatars());
                return new AzureProfilePictureStorage(container);
            } catch (RuntimeException ex) {
                log.warn("Could not use Azure Blob for profile pictures; using local disk. cause={}", ex.toString());
            }
        }
        log.info("Local filesystem storage enabled for profile pictures");
        return new LocalProfilePictureStorage(local);
    }
}
