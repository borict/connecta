package connecta.post.storage;

import connecta.post.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LocalPostImageStorage implements PostImageStorage {

    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Path postsDir;
    private final String publicBaseUrl;

    public LocalPostImageStorage(StorageProperties properties) throws IOException {
        this.postsDir = Path.of(properties.baseDir(), "posts").toAbsolutePath().normalize();
        this.publicBaseUrl = trimTrailingSlash(properties.publicBaseUrl());
        Files.createDirectories(this.postsDir);
    }

    @Override
    public String store(UUID postId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be at most 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be JPEG, PNG, or WebP");
        }

        delete(postId);

        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase(Locale.ROOT));
        String filename = postId + "." + extension;
        Path destination = postsDir.resolve(filename).normalize();
        if (!destination.startsWith(postsDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image path");
        }

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image", ex);
        }

        return publicBaseUrl + "/posts/" + filename;
    }

    @Override
    public void delete(UUID postId) {
        for (String extension : EXTENSION_BY_CONTENT_TYPE.values()) {
            Path file = postsDir.resolve(postId + "." + extension).normalize();
            if (!file.startsWith(postsDir)) {
                continue;
            }
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // Best-effort cleanup; the post row is the source of truth.
            }
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
