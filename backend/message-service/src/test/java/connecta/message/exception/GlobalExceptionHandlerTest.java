package connecta.message.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void responseStatusException_mapsToApiErrorResponse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("X-B3-TraceId")).thenReturn("abc123");

        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("Conversation not found");
        assertThat(body.path()).isEqualTo("/api/conversations");
        assertThat(body.traceId()).isEqualTo("abc123");
        assertThat(body.timestamp()).isNotNull();
    }
}
