package at.qe.skeleton.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConfigRequestDTO(
        UUID raspberryPi,
        LocalDateTime triggeredAt
) {
}
