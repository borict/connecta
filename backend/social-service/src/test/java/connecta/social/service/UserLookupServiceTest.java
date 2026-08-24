package connecta.social.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import connecta.social.client.UserClient;
import connecta.social.client.UserSummaryDto;
import java.util.List;
import java.util.UUID;
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
}
