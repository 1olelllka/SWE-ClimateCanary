package at.qe.skeleton.dtos;

import java.time.LocalDate;

public record AggregatedDataPointDTO(
        LocalDate date,
        double avgTemperature,
        double avgHumidity,
        double avgAirQuality
) {
}
