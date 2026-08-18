package connecta.gateway.security;

public record JwtPrincipal(
        String userId,
        String username,
        String role
) {
}
