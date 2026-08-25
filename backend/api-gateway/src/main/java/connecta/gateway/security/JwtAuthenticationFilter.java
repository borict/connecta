package connecta.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final GatewayAuthSupport authSupport;

    public JwtAuthenticationFilter(JwtService jwtService, GatewayAuthSupport authSupport) {
        this.jwtService = jwtService;
        this.authSupport = authSupport;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest cleaned = stripClientAuthHeaders(exchange.getRequest());

        if (authSupport.isPublic(cleaned)) {
            return chain.filter(exchange.mutate().request(cleaned).build());
        }

        String token = authSupport.extractToken(cleaned);
        if (token == null) {
            return authSupport.writeError(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        var principalOpt = jwtService.parse(token);
        if (principalOpt.isEmpty()) {
            return authSupport.writeError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        JwtPrincipal principal = principalOpt.get();
        if (authSupport.isAdminPath(cleaned) && !"ADMIN".equals(principal.role())) {
            return authSupport.writeError(exchange, HttpStatus.FORBIDDEN, "Access denied");
        }

        ServerHttpRequest authenticated = authSupport.withoutQueryToken(cleaned).mutate()
                .header(AuthHeaders.USER_ID, principal.userId())
                .header(AuthHeaders.USERNAME, principal.username())
                .header(AuthHeaders.USER_ROLE, principal.role())
                .build();

        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private ServerHttpRequest stripClientAuthHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(AuthHeaders.USER_ID);
                    headers.remove(AuthHeaders.USERNAME);
                    headers.remove(AuthHeaders.USER_ROLE);
                    headers.remove("X-User-Id");
                    headers.remove("X-Username");
                    headers.remove("X-User-Role");
                })
                .build();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
