package connecta.post.storage;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface PostImageStorage {

    String store(UUID postId, MultipartFile file);

    void delete(UUID postId);
}
