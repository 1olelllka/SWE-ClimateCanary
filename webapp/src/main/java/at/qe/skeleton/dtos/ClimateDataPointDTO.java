package at.qe.skeleton.dtos;

import java.time.OffsetDateTime;

public record ClimateDataPointDTO(
        OffsetDateTime timestamp,
        double temperature,
        double humidity,
        double airQuality
) {
}
