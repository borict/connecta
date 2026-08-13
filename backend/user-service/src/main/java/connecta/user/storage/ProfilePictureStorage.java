package connecta.user.storage;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores profile pictures and returns a public URL for {@code profilePictureUrl}.
 * Local filesystem now; can be swapped for Azure Blob later.
 */
public interface ProfilePictureStorage {

    String store(UUID userId, MultipartFile file);
}
