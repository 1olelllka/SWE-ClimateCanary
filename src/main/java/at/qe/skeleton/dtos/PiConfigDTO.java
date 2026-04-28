package at.qe.skeleton.dtos;

import java.util.Set;
import java.util.UUID;

public record PiConfigDTO(
        UUID roomId,
        UUID raspberryId,
        int frequency,
        LimitDTO limits,
        Set<ReducedSensorDTO> sensors
) {
}
