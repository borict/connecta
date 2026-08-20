package connecta.post.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import connecta.post.config.StorageProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class LocalPostImageStorageTest {

    @TempDir
    Path tempDir;

    private LocalPostImageStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new LocalPostImageStorage(new StorageProperties(
                tempDir.toString(),
                "http://localhost:8080/media"
        ));
    }

    @Test
    void store_writesJpegAndReturnsPublicUrl() throws Exception {
        UUID postId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "photo.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );

        String url = storage.store(postId, file);

        assertThat(url).isEqualTo("http://localhost:8080/media/posts/" + postId + ".jpg");
        assertThat(Files.exists(tempDir.resolve("posts").resolve(postId + ".jpg"))).isTrue();
    }

    @Test
    void store_rejectsUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "notes.txt",
                "text/plain",
                new byte[] {1}
        );

        assertThatThrownBy(() -> storage.store(UUID.randomUUID(), file))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
