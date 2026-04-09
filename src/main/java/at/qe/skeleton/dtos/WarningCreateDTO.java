package at.qe.skeleton.dtos;

import at.qe.skeleton.model.MeasurementType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WarningCreateDTO(

        @NotNull(message = "Room ID must not be null.")
        @NotEmpty(message = "Room ID must not be empty.")
        UUID roomId,

        @NotNull(message = "Sensor type must not be null.")
        MeasurementType sensorType,

        @NotNull(message = "Value must not be null.")
        @NotEmpty(message = "Value must not be empty.")
        double value,

        @NotNull(message = "Limit must not be null.")
        @NotEmpty(message = "Limit must not be empty.")
        double limitExceeded,

        @NotEmpty(message = "Message must not be empty.") //thought can be null if not set
        String message
) {
}
