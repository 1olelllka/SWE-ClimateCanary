package at.qe.skeleton.dtos;

import at.qe.skeleton.model.MeasurementType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ReadingDTO(
        @NotNull(message = "Reading type must not be null.")
        @NotEmpty(message = "Reading type must not be empty.")
        MeasurementType type,

        @NotNull(message = "Reading value must not be null.")
        double value
) {
}
