package at.qe.skeleton.dtos;

import java.util.List;
import java.util.UUID;

public record PiConfigDTO(
        int frequency,
        List<UUID> sensors
) {
}
