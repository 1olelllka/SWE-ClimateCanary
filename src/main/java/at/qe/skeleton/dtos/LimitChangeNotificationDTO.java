package at.qe.skeleton.dtos;

import java.time.LocalDateTime;

public record LimitChangeNotificationDTO(
        Float tempMin,
        Float tempMax,
        Float humMin,
        Float humMax,
        Float co2Max,
        LocalDateTime triggeredAt
) {
}
