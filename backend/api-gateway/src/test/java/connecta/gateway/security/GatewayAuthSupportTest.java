package connecta.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class GatewayAuthSupportTest {

    private final GatewayAuthSupport support = new GatewayAuthSupport(new ObjectMapper());

    @Test
    void publicPaths_doNotRequireJwt() {
        assertThat(support.isPublic(request(HttpMethod.POST, "/api/auth/register"))).isTrue();
        assertThat(support.isPublic(request(HttpMethod.POST, "/api/auth/login"))).isTrue();
        assertThat(support.isPublic(request(HttpMethod.GET, "/media/profile-pictures/x.jpg"))).isTrue();
        assertThat(support.isPublic(request(HttpMethod.GET, "/media/posts/x.jpg"))).isTrue();
        assertThat(support.isPublic(request(HttpMethod.GET, "/actuator/health"))).isTrue();
    }

    @Test
    void protectedPaths_requireJwt() {
        assertThat(support.isPublic(request(HttpMethod.GET, "/api/users/me"))).isFalse();
        assertThat(support.isPublic(request(HttpMethod.GET, "/api/admin/users"))).isFalse();
        assertThat(support.isAdminPath(request(HttpMethod.GET, "/api/admin/users"))).isTrue();
        assertThat(support.isPublic(request(HttpMethod.POST, "/api/posts"))).isFalse();
        assertThat(support.isPublic(request(HttpMethod.GET, "/api/posts/by-authors"))).isFalse();
        assertThat(support.isPublic(request(HttpMethod.GET, "/api/posts/user/11111111-1111-1111-1111-111111111111"))).isFalse();
        assertThat(support.isPublic(request(HttpMethod.DELETE, "/api/posts/11111111-1111-1111-1111-111111111111"))).isFalse();
        assertThat(support.isPublic(request(HttpMethod.POST, "/api/social/11111111-1111-1111-1111-111111111111"))).isFalse();
        assertThat(support.isPublic(request(HttpMethod.GET, "/api/social/me/following"))).isFalse();
        assertThat(support.isPublic(request(HttpMethod.GET, "/api/feed"))).isFalse();
    }

    private static ServerHttpRequest request(HttpMethod method, String path) {
        return MockServerHttpRequest.method(method, "http://localhost:8080" + path).build();
    }
}
