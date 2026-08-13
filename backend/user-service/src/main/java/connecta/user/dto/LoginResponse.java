package connecta.user.dto;

public record LoginResponse(
        String token,
        UserMeResponse user
) {
}
