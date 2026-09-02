package connecta.user.storage;

import connecta.user.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public class LocalProfilePictureStorage implements ProfilePictureStorage {

    private final Path profilePicturesDir;
    private final String publicBaseUrl;

    public LocalProfilePictureStorage(StorageProperties properties) throws IOException {
        this.profilePicturesDir = Path.of(properties.baseDir(), "profile-pictures").toAbsolutePath().normalize();
        this.publicBaseUrl = trimTrailingSlash(properties.publicBaseUrl());
        Files.createDirectories(this.profilePicturesDir);
    }

    @Override
    public String store(UUID userId, MultipartFile file) {
        String extension = ProfilePictureUploads.extensionFor(file);
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
