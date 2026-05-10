package at.qe.skeleton.dtos;

import at.qe.skeleton.model.MeasurementType;
import at.qe.skeleton.model.WarningStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WarningCreateDTO(

        @NotNull(message = "Room ID must not be null.")
        UUID roomId,

        String device,

        @NotNull(message = "Sensor type must not be null.")
        MeasurementType measurementType,

        @NotNull(message = "Warning status must not be null.")
        WarningStatus status,

        @NotNull(message = "Value must not be null.")
        double triggeredValue,

        @NotNull(message = "Limit must not be null.")
        double activeLimitAtTime,

        @NotEmpty(message = "Message must not be empty.") //thought can be null if not set
        String message
) {
}
