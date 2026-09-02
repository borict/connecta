package connecta.user.dto;

import java.util.List;
import java.util.UUID;

public record PublicIdsResponse(List<UUID> ids) {
}
