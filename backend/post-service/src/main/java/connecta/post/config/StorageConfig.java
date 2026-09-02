package connecta.post.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import connecta.post.storage.AzurePostImageStorage;
import connecta.post.storage.LocalPostImageStorage;
import connecta.post.storage.PostImageStorage;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    PostImageStorage postImageStorage(
            AzureStorageProperties azure,
            StorageProperties local
    ) throws IOException {
        if (azure.isConfigured()) {
            try {
                BlobContainerClient container = new BlobServiceClientBuilder()
                        .connectionString(azure.connectionString())
                        .buildClient()
                        .getBlobContainerClient(azure.containerPosts());
                container.createIfNotExists();
                log.info("Azure Blob storage enabled for post images; container={}", azure.containerPosts());
                return new AzurePostImageStorage(container);
            } catch (RuntimeException ex) {
                log.warn("Could not use Azure Blob for post images; using local disk. cause={}", ex.toString());
            }
        }
        log.info("Local filesystem storage enabled for post images");
        return new LocalPostImageStorage(local);
    }
}
