package at.qe.skeleton.dtos;

import java.util.UUID;

public record ReducedSensorDTO(
        String name,
        UUID readId,
        UUID writeId
) {
}
