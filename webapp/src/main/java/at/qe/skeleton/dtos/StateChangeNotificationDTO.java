package at.qe.skeleton.dtos;

import java.time.LocalDateTime;

public record StateChangeNotificationDTO(
    UpdateType updateType,
    LocalDateTime triggeredAt
) {
}
