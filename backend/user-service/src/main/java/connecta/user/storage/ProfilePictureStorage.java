package connecta.user.storage;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ProfilePictureStorage {

    String store(UUID userId, MultipartFile file);
}
