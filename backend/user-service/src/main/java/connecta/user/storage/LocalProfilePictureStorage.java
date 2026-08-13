package connecta.user.storage;

import connecta.user.config.StorageProperties;
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
public class LocalProfilePictureStorage implements ProfilePictureStorage {

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

    private final Path profilePicturesDir;
    private final String publicBaseUrl;

    public LocalProfilePictureStorage(StorageProperties properties) throws IOException {
        this.profilePicturesDir = Path.of(properties.baseDir(), "profile-pictures").toAbsolutePath().normalize();
        this.publicBaseUrl = trimTrailingSlash(properties.publicBaseUrl());
        Files.createDirectories(this.profilePicturesDir);
    }

    @Override
    public String store(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile picture file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile picture must be at most 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Profile picture must be JPEG, PNG, or WebP"
            );
        }

        String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType.toLowerCase(Locale.ROOT));
        String filename = userId + "." + extension;
        Path destination = profilePicturesDir.resolve(filename).normalize();
        if (!destination.startsWith(profilePicturesDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid profile picture path");
        }

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store profile picture",
                    ex
            );
        }

        return publicBaseUrl + "/profile-pictures/" + filename;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
