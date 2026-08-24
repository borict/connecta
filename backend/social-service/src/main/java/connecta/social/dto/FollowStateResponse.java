package connecta.social.dto;

public record FollowStateResponse(
        boolean following,
        boolean pending
) {
}
