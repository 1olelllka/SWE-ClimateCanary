package at.qe.skeleton.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record StateChangeNotificationDTO(
    UpdateType updateType,
    LocalDateTime triggeredAt
) {
}
