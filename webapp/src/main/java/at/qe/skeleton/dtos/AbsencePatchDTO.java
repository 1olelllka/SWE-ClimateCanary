package at.qe.skeleton.dtos;

import at.qe.skeleton.model.AbsenceStatus;
import jakarta.validation.constraints.NotNull;

public record AbsencePatchDTO(
        @NotNull(message = "Status must not be null.")
        AbsenceStatus status
) {
}
