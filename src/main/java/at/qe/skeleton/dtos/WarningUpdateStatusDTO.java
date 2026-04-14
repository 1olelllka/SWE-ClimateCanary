package at.qe.skeleton.dtos;

import at.qe.skeleton.model.WarningStatus;
import jakarta.validation.constraints.NotNull;

public record WarningUpdateStatusDTO(
        @NotNull(message = "Warning status should not be null.")
        WarningStatus status,
        @NotNull(message = "Triggered value should not be null.")
        double triggeredValue
) {
}
