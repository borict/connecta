package connecta.user.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class AzureProfilePictureStorageTest {

    private BlobContainerClient container;
    private BlobClient blob;
    private AzureProfilePictureStorage storage;

    @BeforeEach
    void setUp() {
        container = mock(BlobContainerClient.class);
        blob = mock(BlobClient.class);
        storage = new AzureProfilePictureStorage(container);
    }

    @Test
    void store_uploadsAndReturnsBlobUrl() {
        UUID userId = UUID.randomUUID();
        String filename = userId + ".jpg";
        String blobUrl = "https://connecta.blob.core.windows.net/avatars/" + filename;
        when(container.getBlobClient(filename)).thenReturn(blob);
        when(blob.getBlobUrl()).thenReturn(blobUrl);

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "me.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );

        String url = storage.store(userId, file);

        assertThat(url).isEqualTo(blobUrl);
        verify(blob).upload(any(InputStream.class), eq(3L), eq(true));
        verify(blob).setHttpHeaders(any(BlobHttpHeaders.class));
    }

    @Test
    void store_rejectsUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
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
