package at.qe.skeleton.dtos;

import java.util.UUID;

public record OccupancyDTO(
        int effectiveOccupancy,
        UUID roomId,
        boolean privacyMode
) {
}
