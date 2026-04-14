package at.qe.skeleton.dtos;

import java.time.LocalDateTime;

public record ClimateDataPointDTO(
        LocalDateTime timestamp,
        double temperature,
        double humidity,
        double airQuality
) {
}
