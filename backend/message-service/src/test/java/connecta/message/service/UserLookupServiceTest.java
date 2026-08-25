package connecta.message.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import connecta.message.client.UserClient;
import connecta.message.client.UserSummaryDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    @Mock
    private UserClient userClient;

    private UserLookupService lookup;

    @BeforeEach
    void setUp() {
        lookup = new UserLookupService(userClient);
    }

    @Test
    void requireActiveUser_mapsBatchResult() {
        UUID userId = UUID.randomUUID();
        when(userClient.batchUsers(userId.toString())).thenReturn(List.of(
                new UserSummaryDto(userId, "ana", "Ana", null, true)
        ));

        UserSummaryDto result = lookup.requireActiveUser(userId);

        assertThat(result.username()).isEqualTo("ana");
        assertThat(result.isPrivate()).isTrue();
    }

    @Test
    void requireActiveUser_missing_returnsNotFound() {
        UUID userId = UUID.randomUUID();
        when(userClient.batchUsers(userId.toString())).thenReturn(List.of());

        assertThatThrownBy(() -> lookup.requireActiveUser(userId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void requireActiveUser_userServiceDown_returnsServiceUnavailable() {
        UUID userId = UUID.randomUUID();
        when(userClient.batchUsers(userId.toString())).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> lookup.requireActiveUser(userId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException statusEx = (ResponseStatusException) ex;
                    assertThat(statusEx.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(statusEx.getReason()).isEqualTo("User Service unavailable");
                });
    }

    @Test
    void summariesByIds_mapsBatchResult() {
        UUID userId = UUID.randomUUID();
        when(userClient.batchUsers(userId.toString())).thenReturn(List.of(
                new UserSummaryDto(userId, "ana", "Ana", "http://pic", false)
        ));

        Map<UUID, UserSummaryDto> result = lookup.summariesByIds(List.of(userId));

        assertThat(result.get(userId).username()).isEqualTo("ana");
        assertThat(result.get(userId).displayName()).isEqualTo("Ana");
    }

    @Test
    void summariesByIds_userServiceDown_returnsEmptyMap() {
        UUID userId = UUID.randomUUID();
        when(userClient.batchUsers(anyString())).thenThrow(new RuntimeException("connection refused"));

        Map<UUID, UserSummaryDto> result = lookup.summariesByIds(List.of(userId));

        assertThat(result).isEmpty();
    }

    @Test
    void summariesByIds_chunksRequestsOverMaxBatchSize() {
        List<UUID> ids = IntStream.range(0, 101)
                .mapToObj(i -> UUID.randomUUID())
                .toList();
        when(userClient.batchUsers(anyString())).thenReturn(List.of());

        lookup.summariesByIds(ids);

        verify(userClient, times(2)).batchUsers(anyString());
    }
}
