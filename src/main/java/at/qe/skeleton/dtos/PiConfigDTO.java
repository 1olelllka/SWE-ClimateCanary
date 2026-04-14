package at.qe.skeleton.dtos;

import java.util.UUID;

public record PiConfigDTO(
        int frequency,
        UUID sensor
) {
}
