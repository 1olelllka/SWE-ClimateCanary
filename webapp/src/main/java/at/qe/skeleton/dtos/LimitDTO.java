package at.qe.skeleton.dtos;

import java.util.UUID;

public record LimitDTO(
        UUID roomId,

        Float tempMin,

        Float tempMax,

        Float humMin,

        Float humMax,

        Float co2Max
) {
}
