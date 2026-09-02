package connecta.user.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public class AzureProfilePictureStorage implements ProfilePictureStorage {

    private final BlobContainerClient container;

    public AzureProfilePictureStorage(BlobContainerClient container) {
        this.container = container;
    }

    @Override
    public String store(UUID userId, MultipartFile file) {
        String extension = ProfilePictureUploads.extensionFor(file);
        String filename = userId + "." + extension;
        BlobClient blob = container.getBlobClient(filename);
        try (InputStream in = file.getInputStream()) {
            blob.upload(in, file.getSize(), true);
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store profile picture",
                    ex
            );
        }
        blob.setHttpHeaders(new BlobHttpHeaders().setContentType(file.getContentType()));
        return blob.getBlobUrl();
    }
}
