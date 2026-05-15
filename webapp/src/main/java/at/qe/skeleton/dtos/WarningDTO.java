package at.qe.skeleton.dtos;

import at.qe.skeleton.model.MeasurementType;
import at.qe.skeleton.model.WarningStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record WarningDTO(
        UUID id,
        UUID roomId,
        UUID writeId,
        MeasurementType measurementType,
        WarningStatus status,
        String message,
        double triggeredValue,
        double activeLimitAtTime,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,   // null if still active
        String tip,
        boolean active
) {
}
