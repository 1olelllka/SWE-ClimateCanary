package at.qe.skeleton.dtos;

import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;
import at.qe.skeleton.model.WarningStatus;

import java.util.UUID;

public record TipDTO(
        UUID id,
        WarningStatus violationStatus,
        ViolationType violationType,
        ViolatedSensor violatedSensor,
        String message
) {
}
