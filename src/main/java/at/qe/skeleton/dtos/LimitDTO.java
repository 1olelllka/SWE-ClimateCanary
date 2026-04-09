package at.qe.skeleton.dtos;

import java.util.UUID;

public record LimitDTO(
        UUID roomId,

        float tempMin,

        float tempMax,

        float humMax,

        float humMin,

        float co2Max
) {
}
