package at.qe.skeleton.dtos;

import at.qe.skeleton.model.ViolatedSensor;
import at.qe.skeleton.model.ViolationType;

import java.util.UUID;

public record TipDTO(
        UUID id,
        UUID roomId,
        ViolationType violationType,
        ViolatedSensor violatedSensor,
        String message
) {
}
