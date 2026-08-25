package connecta.message.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSummaryDto(
        UUID id,
        String username,
        String displayName,
        String profilePictureUrl,
        boolean isPrivate
) {
}
