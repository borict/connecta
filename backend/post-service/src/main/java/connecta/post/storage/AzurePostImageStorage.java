package connecta.post.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public class AzurePostImageStorage implements PostImageStorage {

    private final BlobContainerClient container;

    public AzurePostImageStorage(BlobContainerClient container) {
        this.container = container;
    }

    @Override
    public String store(UUID postId, MultipartFile file) {
        String extension = PostImageUploads.extensionFor(file);
        delete(postId);

        String filename = postId + "." + extension;
        BlobClient blob = container.getBlobClient(filename);
        try (InputStream in = file.getInputStream()) {
            blob.upload(in, file.getSize(), true);
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image", ex);
        }
        blob.setHttpHeaders(new BlobHttpHeaders().setContentType(file.getContentType()));
        return blob.getBlobUrl();
    }

    @Override
    public void delete(UUID postId) {
        for (String extension : PostImageUploads.EXTENSIONS) {
            try {
                container.getBlobClient(postId + "." + extension).deleteIfExists();
            } catch (RuntimeException ignored) {
                // Best-effort cleanup; the post row is the source of truth.
            }
        }
    }
}
