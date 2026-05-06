package at.qe.skeleton.dtos;

import at.qe.skeleton.model.MeasurementType;

import java.time.LocalDateTime;

public record ViolationLogDTO(
        LocalDateTime timestamp,
        MeasurementType sensorType,
        double value,
        double limitViolated,
        String duration
) {
}
