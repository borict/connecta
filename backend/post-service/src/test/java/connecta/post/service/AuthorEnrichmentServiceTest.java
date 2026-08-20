package connecta.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.post.client.UserClient;
import connecta.post.client.UserSummaryDto;
import connecta.post.dto.AuthorSummary;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorEnrichmentServiceTest {

    @Mock
    private UserClient userClient;

    private AuthorEnrichmentService enrichment;

    @BeforeEach
    void setUp() {
        enrichment = new AuthorEnrichmentService(userClient);
    }

    @Test
    void byIds_mapsUserSummaries() {
        UUID authorId = UUID.randomUUID();
        when(userClient.batchUsers(authorId.toString())).thenReturn(List.of(
                new UserSummaryDto(authorId, "tamara", "Tamara", "http://pic")
        ));

        Map<UUID, AuthorSummary> result = enrichment.byIds(List.of(authorId));

        assertThat(result.get(authorId).username()).isEqualTo("tamara");
        assertThat(result.get(authorId).displayName()).isEqualTo("Tamara");
        assertThat(result.get(authorId).profilePictureUrl()).isEqualTo("http://pic");
    }

    @Test
    void byIds_userServiceDown_returnsEmptyMap() {
        UUID authorId = UUID.randomUUID();
        when(userClient.batchUsers(anyString())).thenThrow(new RuntimeException("connection refused"));

        Map<UUID, AuthorSummary> result = enrichment.byIds(List.of(authorId));

        assertThat(result).isEmpty();
    }

    @Test
    void byIds_chunksRequestsOverMaxBatchSize() {
        List<UUID> ids = IntStream.range(0, 101)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        when(userClient.batchUsers(anyString())).thenReturn(List.of());

        enrichment.byIds(ids);

        verify(userClient, times(2)).batchUsers(anyString());
    }
}
