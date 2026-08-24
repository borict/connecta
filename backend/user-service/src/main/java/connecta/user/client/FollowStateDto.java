package connecta.user.client;

public record FollowStateDto(
        boolean following,
        boolean pending
) {
}
