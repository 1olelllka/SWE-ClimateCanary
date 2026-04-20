package at.qe.skeleton.dtos;

import java.util.Set;
import java.util.UUID;

public record PiConfigDTO(
        int frequency,
        Set<UUID> sensors
) {
}
